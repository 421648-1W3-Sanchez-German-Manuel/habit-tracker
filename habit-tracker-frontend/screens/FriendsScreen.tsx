import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { AddFriendModal } from '../components/AddFriendModal';
import { friendshipService } from '../services/friendshipService';
import { useAuthStore } from '../store/authStore';
import { isApiError } from '../types/api';
import type { FriendshipOverview, UserSummary } from '../types/friendship';
import type { FriendsTopTabParamList } from '../types/navigation';
import { FriendsListScreen } from './FriendsListScreen';
import { IncomingRequestsScreen } from './IncomingRequestsScreen';
import { OutgoingRequestsScreen } from './OutgoingRequestsScreen';

const Tab = createMaterialTopTabNavigator<FriendsTopTabParamList>();

const EMPTY_OVERVIEW: FriendshipOverview = {
  friends: [],
  incomingRequests: [],
  outgoingRequests: [],
};

const removeUserById = (users: UserSummary[], id: string) => users.filter((u) => u.id !== id);

const upsertUser = (users: UserSummary[], user: UserSummary) => {
  const filtered = users.filter((u) => u.id !== user.id);
  return [user, ...filtered];
};

export const FriendsScreen = () => {
  const insets = useSafeAreaInsets();
  const token = useAuthStore((state) => state.token);
  const hasHydrated = useAuthStore((state) => state.hasHydrated);
  const isFirstFocus = useRef(true);

  const [overview, setOverview] = useState<FriendshipOverview>(EMPTY_OVERVIEW);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pendingUserIds, setPendingUserIds] = useState<string[]>([]);
  const [addModalVisible, setAddModalVisible] = useState(false);
  const [sendingRequest, setSendingRequest] = useState(false);

  const markPending = useCallback((userId: string) => {
    setPendingUserIds((prev) => (prev.includes(userId) ? prev : [...prev, userId]));
  }, []);

  const clearPending = useCallback((userId: string) => {
    setPendingUserIds((prev) => prev.filter((id) => id !== userId));
  }, []);

  const loadOverview = useCallback(
    async (isManualRefresh = false) => {
      if (!token) {
        setOverview(EMPTY_OVERVIEW);
        setLoading(false);
        setRefreshing(false);
        return;
      }

      if (isManualRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }
      setError(null);

      try {
        const data = await friendshipService.getOverview(token);
        setOverview(data);
      } catch (loadError) {
        const message = isApiError(loadError)
          ? loadError.message
          : 'Could not load your friends. Please try again.';
        setError(message);
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [token]
  );

  useEffect(() => {
    if (!hasHydrated) {
      return;
    }
    void loadOverview();
  }, [hasHydrated, loadOverview]);

  useFocusEffect(
    useCallback(() => {
      if (isFirstFocus.current) {
        isFirstFocus.current = false;
        return;
      }

      if (!hasHydrated || !token) {
        return;
      }
      void loadOverview(true);
    }, [hasHydrated, loadOverview, token])
  );

  const handleSendRequest = useCallback(
    async (username: string) => {
      if (!token || sendingRequest) {
        return;
      }

      setSendingRequest(true);
      try {
        const target = await friendshipService.sendRequest(username, token);
        setOverview((prev) => ({
          ...prev,
          outgoingRequests: upsertUser(prev.outgoingRequests, target),
        }));
        setAddModalVisible(false);
      } catch (sendError) {
        const message = isApiError(sendError)
          ? sendError.message
          : 'Could not send friend request. Please try again.';
        Alert.alert('Could not send request', message);
      } finally {
        setSendingRequest(false);
      }
    },
    [sendingRequest, token]
  );

  const handleAcceptRequest = useCallback(
    async (requester: UserSummary) => {
      if (!token || pendingUserIds.includes(requester.id)) {
        return;
      }

      const previous = overview;
      markPending(requester.id);
      setOverview((prev) => ({
        ...prev,
        incomingRequests: removeUserById(prev.incomingRequests, requester.id),
        friends: upsertUser(prev.friends, requester),
      }));

      try {
        await friendshipService.acceptRequest(requester.id, token);
      } catch (acceptError) {
        setOverview(previous);
        const message = isApiError(acceptError)
          ? acceptError.message
          : 'Could not accept the request. Please try again.';
        Alert.alert('Could not accept', message);
      } finally {
        clearPending(requester.id);
      }
    },
    [clearPending, markPending, overview, pendingUserIds, token]
  );

  const handleDeclineRequest = useCallback(
    async (requester: UserSummary) => {
      if (!token || pendingUserIds.includes(requester.id)) {
        return;
      }

      const previous = overview;
      markPending(requester.id);
      setOverview((prev) => ({
        ...prev,
        incomingRequests: removeUserById(prev.incomingRequests, requester.id),
      }));

      try {
        await friendshipService.declineRequest(requester.id, token);
      } catch (declineError) {
        setOverview(previous);
        const message = isApiError(declineError)
          ? declineError.message
          : 'Could not decline the request. Please try again.';
        Alert.alert('Could not decline', message);
      } finally {
        clearPending(requester.id);
      }
    },
    [clearPending, markPending, overview, pendingUserIds, token]
  );

  const handleCancelOutgoing = useCallback(
    async (target: UserSummary) => {
      if (!token || pendingUserIds.includes(target.id)) {
        return;
      }

      const previous = overview;
      markPending(target.id);
      setOverview((prev) => ({
        ...prev,
        outgoingRequests: removeUserById(prev.outgoingRequests, target.id),
      }));

      try {
        await friendshipService.cancelOutgoingRequest(target.id, token);
      } catch (cancelError) {
        setOverview(previous);
        const message = isApiError(cancelError)
          ? cancelError.message
          : 'Could not cancel the request. Please try again.';
        Alert.alert('Could not cancel', message);
      } finally {
        clearPending(target.id);
      }
    },
    [clearPending, markPending, overview, pendingUserIds, token]
  );

  const handleUnfriend = useCallback(
    async (friend: UserSummary) => {
      if (!token || pendingUserIds.includes(friend.id)) {
        return;
      }

      const performUnfriend = async () => {
        const previous = overview;
        markPending(friend.id);
        setOverview((prev) => ({
          ...prev,
          friends: removeUserById(prev.friends, friend.id),
        }));

        try {
          await friendshipService.unfriend(friend.id, token);
        } catch (unfriendError) {
          setOverview(previous);
          const message = isApiError(unfriendError)
            ? unfriendError.message
            : 'Could not remove friend. Please try again.';
          Alert.alert('Could not unfriend', message);
        } finally {
          clearPending(friend.id);
        }
      };

      Alert.alert(
        'Remove friend?',
        `${friend.username} will be removed from your friends list.`,
        [
          { text: 'Cancel', style: 'cancel' },
          { text: 'Remove', style: 'destructive', onPress: () => void performUnfriend() },
        ]
      );
    },
    [clearPending, markPending, overview, pendingUserIds, token]
  );

  return (
    <View style={[styles.container, { paddingTop: Math.max(insets.top, 8) }]}>
      <View style={styles.header}>
        <View>
          <Text style={styles.headerTitle}>Friends</Text>
          <Text style={styles.headerSubtitle}>
            {overview.friends.length} friend{overview.friends.length === 1 ? '' : 's'} ·{' '}
            {overview.incomingRequests.length} pending
          </Text>
        </View>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Add a friend"
          onPress={() => setAddModalVisible(true)}
          style={({ pressed }) => [styles.addButton, pressed && styles.addButtonPressed]}
        >
          <MaterialCommunityIcons name="account-plus-outline" size={20} color="#ffffff" />
          <Text style={styles.addButtonText}>Add</Text>
        </Pressable>
      </View>

      <Tab.Navigator
        initialRouteName="FriendsList"
        screenOptions={{
          swipeEnabled: true,
          animationEnabled: true,
          tabBarActiveTintColor: '#0f172a',
          tabBarInactiveTintColor: '#64748b',
          tabBarPressColor: 'transparent',
          tabBarStyle: styles.tabBar,
          tabBarItemStyle: styles.tabBarItem,
          tabBarLabelStyle: styles.tabBarLabel,
          tabBarIndicatorStyle: styles.tabBarIndicator,
        }}
      >
        <Tab.Screen name="FriendsList" options={{ title: 'Friends' }}>
          {() => (
            <FriendsListScreen
              friends={overview.friends}
              loading={loading}
              refreshing={refreshing}
              pendingUserIds={pendingUserIds}
              error={error}
              onRetry={() => void loadOverview(true)}
              onUnfriend={handleUnfriend}
            />
          )}
        </Tab.Screen>
        <Tab.Screen name="IncomingRequests" options={{ title: 'Incoming' }}>
          {() => (
            <IncomingRequestsScreen
              requests={overview.incomingRequests}
              loading={loading}
              refreshing={refreshing}
              pendingUserIds={pendingUserIds}
              error={error}
              onRetry={() => void loadOverview(true)}
              onAccept={handleAcceptRequest}
              onDecline={handleDeclineRequest}
            />
          )}
        </Tab.Screen>
        <Tab.Screen name="OutgoingRequests" options={{ title: 'Outgoing' }}>
          {() => (
            <OutgoingRequestsScreen
              requests={overview.outgoingRequests}
              loading={loading}
              refreshing={refreshing}
              pendingUserIds={pendingUserIds}
              error={error}
              onRetry={() => void loadOverview(true)}
              onCancel={handleCancelOutgoing}
            />
          )}
        </Tab.Screen>
      </Tab.Navigator>

      <AddFriendModal
        visible={addModalVisible}
        submitting={sendingRequest}
        onCancel={() => {
          if (!sendingRequest) {
            setAddModalVisible(false);
          }
        }}
        onSubmit={handleSendRequest}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8fafc',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 12,
  },
  headerTitle: {
    fontSize: 22,
    fontWeight: '800',
    color: '#0f172a',
  },
  headerSubtitle: {
    marginTop: 2,
    fontSize: 12,
    color: '#64748b',
    fontWeight: '600',
  },
  addButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: '#0f766e',
    paddingHorizontal: 14,
    paddingVertical: 9,
    borderRadius: 10,
  },
  addButtonPressed: {
    opacity: 0.85,
  },
  addButtonText: {
    color: '#ffffff',
    fontWeight: '700',
    fontSize: 14,
  },
  tabBar: {
    marginHorizontal: 16,
    marginTop: 4,
    marginBottom: 8,
    borderRadius: 12,
    backgroundColor: '#e2e8f0',
    elevation: 0,
    shadowOpacity: 0,
  },
  tabBarItem: {
    minHeight: 40,
    borderRadius: 10,
  },
  tabBarLabel: {
    fontSize: 13,
    fontWeight: '700',
    textTransform: 'none',
  },
  tabBarIndicator: {
    padding: 16,
    borderRadius: 10,
    backgroundColor: '#ffffff',
    margin: 6,
  },
});

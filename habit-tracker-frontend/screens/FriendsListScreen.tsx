import { ActivityIndicator, FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { UserListItem } from '../components/UserListItem';
import type { UserSummary } from '../types/friendship';

interface FriendsListScreenProps {
  friends: UserSummary[];
  loading: boolean;
  refreshing: boolean;
  pendingUserIds: string[];
  error: string | null;
  onRetry: () => void;
  onUnfriend: (user: UserSummary) => void;
}

export const FriendsListScreen = ({
  friends,
  loading,
  refreshing,
  pendingUserIds,
  error,
  onRetry,
  onUnfriend,
}: FriendsListScreenProps) => {
  if (loading && friends.length === 0) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color="#0f766e" />
        <Text style={styles.subtitle}>Loading friends...</Text>
      </View>
    );
  }

  if (error && friends.length === 0) {
    return (
      <View style={styles.centered}>
        <Text style={styles.title}>Could not load friends</Text>
        <Text style={styles.subtitle}>{error}</Text>
        <Pressable onPress={onRetry} style={({ pressed }) => [styles.retryButton, pressed && styles.pressed]}>
          <Text style={styles.retryButtonText}>Try again</Text>
        </Pressable>
      </View>
    );
  }

  return (
    <FlatList
      data={friends}
      keyExtractor={(item) => item.id}
      contentContainerStyle={friends.length === 0 ? styles.emptyContainer : styles.listContent}
      onRefresh={onRetry}
      refreshing={refreshing}
      renderItem={({ item }) => {
        const isPending = pendingUserIds.includes(item.id);
        return (
          <UserListItem
            user={item}
            trailing={
              <Pressable
                accessibilityRole="button"
                accessibilityLabel={`Remove ${item.username}`}
                onPress={() => onUnfriend(item)}
                disabled={isPending}
                style={({ pressed }) => [
                  styles.unfriendButton,
                  pressed && !isPending && styles.pressed,
                  isPending && styles.disabled,
                ]}
              >
                <MaterialCommunityIcons name="account-remove-outline" size={18} color="#b91c1c" />
              </Pressable>
            }
          />
        );
      }}
      ListEmptyComponent={
        <View style={styles.centered}>
          <Text style={styles.title}>No friends yet</Text>
          <Text style={styles.subtitle}>Use the Add button to send a friend request.</Text>
        </View>
      }
    />
  );
};

const styles = StyleSheet.create({
  centered: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
    backgroundColor: '#f8fafc',
  },
  listContent: {
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 24,
    gap: 10,
  },
  emptyContainer: {
    flexGrow: 1,
  },
  title: {
    fontSize: 20,
    fontWeight: '700',
    color: '#0f172a',
    textAlign: 'center',
  },
  subtitle: {
    marginTop: 8,
    fontSize: 14,
    color: '#475569',
    textAlign: 'center',
  },
  retryButton: {
    marginTop: 16,
    backgroundColor: '#0f766e',
    paddingHorizontal: 18,
    paddingVertical: 10,
    borderRadius: 10,
  },
  retryButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '700',
  },
  unfriendButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#fee2e2',
    alignItems: 'center',
    justifyContent: 'center',
  },
  pressed: {
    opacity: 0.85,
  },
  disabled: {
    opacity: 0.5,
  },
});

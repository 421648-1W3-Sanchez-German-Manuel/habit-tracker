import { ActivityIndicator, FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { UserListItem } from '../components/UserListItem';
import type { UserSummary } from '../types/friendship';

interface IncomingRequestsScreenProps {
  requests: UserSummary[];
  loading: boolean;
  refreshing: boolean;
  pendingUserIds: string[];
  error: string | null;
  onRetry: () => void;
  onAccept: (user: UserSummary) => void;
  onDecline: (user: UserSummary) => void;
}

export const IncomingRequestsScreen = ({
  requests,
  loading,
  refreshing,
  pendingUserIds,
  error,
  onRetry,
  onAccept,
  onDecline,
}: IncomingRequestsScreenProps) => {
  if (loading && requests.length === 0) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color="#0f766e" />
        <Text style={styles.subtitle}>Loading requests...</Text>
      </View>
    );
  }

  if (error && requests.length === 0) {
    return (
      <View style={styles.centered}>
        <Text style={styles.title}>Could not load requests</Text>
        <Text style={styles.subtitle}>{error}</Text>
        <Pressable onPress={onRetry} style={({ pressed }) => [styles.retryButton, pressed && styles.pressed]}>
          <Text style={styles.retryButtonText}>Try again</Text>
        </Pressable>
      </View>
    );
  }

  return (
    <FlatList
      data={requests}
      keyExtractor={(item) => item.id}
      contentContainerStyle={requests.length === 0 ? styles.emptyContainer : styles.listContent}
      onRefresh={onRetry}
      refreshing={refreshing}
      renderItem={({ item }) => {
        const isPending = pendingUserIds.includes(item.id);
        return (
          <UserListItem
            user={item}
            caption="wants to be your friend"
            trailing={
              <>
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel={`Accept ${item.username}`}
                  onPress={() => onAccept(item)}
                  disabled={isPending}
                  style={({ pressed }) => [
                    styles.acceptButton,
                    pressed && !isPending && styles.pressed,
                    isPending && styles.disabled,
                  ]}
                >
                  <Text style={styles.acceptButtonText}>Accept</Text>
                </Pressable>
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel={`Decline ${item.username}`}
                  onPress={() => onDecline(item)}
                  disabled={isPending}
                  style={({ pressed }) => [
                    styles.declineButton,
                    pressed && !isPending && styles.pressed,
                    isPending && styles.disabled,
                  ]}
                >
                  <Text style={styles.declineButtonText}>Decline</Text>
                </Pressable>
              </>
            }
          />
        );
      }}
      ListEmptyComponent={
        <View style={styles.centered}>
          <Text style={styles.title}>No incoming requests</Text>
          <Text style={styles.subtitle}>You'll see new friend requests here.</Text>
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
  acceptButton: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 8,
    backgroundColor: '#0f766e',
  },
  acceptButtonText: {
    color: '#ffffff',
    fontWeight: '700',
    fontSize: 13,
  },
  declineButton: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 8,
    backgroundColor: '#f1f5f9',
    borderWidth: 1,
    borderColor: '#cbd5e1',
  },
  declineButtonText: {
    color: '#334155',
    fontWeight: '700',
    fontSize: 13,
  },
  pressed: {
    opacity: 0.85,
  },
  disabled: {
    opacity: 0.5,
  },
});

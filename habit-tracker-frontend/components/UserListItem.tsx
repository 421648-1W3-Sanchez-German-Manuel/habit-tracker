import { ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import type { UserSummary } from '../types/friendship';

interface UserListItemProps {
  user: UserSummary;
  trailing?: ReactNode;
  caption?: string;
}

const getInitials = (username: string): string => {
  const trimmed = username.trim();
  if (trimmed.length === 0) {
    return '?';
  }
  const parts = trimmed.split(/[\s._-]+/).filter(Boolean);
  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase();
  }
  return (parts[0][0] + parts[1][0]).toUpperCase();
};

export const UserListItem = ({ user, trailing, caption }: UserListItemProps) => {
  return (
    <View style={styles.card}>
      <View style={styles.avatar}>
        <Text style={styles.avatarText}>{getInitials(user.username)}</Text>
      </View>
      <View style={styles.main}>
        <Text style={styles.username} numberOfLines={1}>
          {user.username}
        </Text>
        {caption ? (
          <Text style={styles.caption} numberOfLines={1}>
            {caption}
          </Text>
        ) : null}
      </View>
      {trailing ? <View style={styles.trailing}>{trailing}</View> : null}
    </View>
  );
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#ffffff',
    borderRadius: 14,
    borderWidth: 1,
    borderColor: '#dbe3ee',
    paddingHorizontal: 14,
    paddingVertical: 12,
    flexDirection: 'row',
    alignItems: 'center',
  },
  avatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#ccfbf1',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  avatarText: {
    fontSize: 14,
    fontWeight: '800',
    color: '#0f766e',
  },
  main: {
    flex: 1,
    marginRight: 8,
  },
  username: {
    fontSize: 16,
    fontWeight: '700',
    color: '#0f172a',
  },
  caption: {
    marginTop: 2,
    fontSize: 12,
    color: '#64748b',
  },
  trailing: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
});

import { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { Input } from './Input';

interface AddFriendModalProps {
  visible: boolean;
  submitting: boolean;
  onCancel: () => void;
  onSubmit: (username: string) => Promise<void> | void;
}

export const AddFriendModal = ({ visible, submitting, onCancel, onSubmit }: AddFriendModalProps) => {
  const [username, setUsername] = useState('');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!visible) {
      setUsername('');
      setError(null);
    }
  }, [visible]);

  const handleSubmit = async () => {
    const trimmed = username.trim();
    if (trimmed.length === 0) {
      setError('Username is required');
      return;
    }
    setError(null);
    await onSubmit(trimmed);
  };

  return (
    <Modal animationType="fade" transparent visible={visible} onRequestClose={onCancel}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.overlay}
      >
        <Pressable style={StyleSheet.absoluteFill} onPress={submitting ? undefined : onCancel} />

        <View style={styles.sheet}>
          <Text style={styles.title}>Add a friend</Text>
          <Text style={styles.subtitle}>Type the username you want to send a request to.</Text>

          <View style={styles.inputWrapper}>
            <Input
              label="Username"
              placeholder="e.g. lucia_ramos"
              value={username}
              onChangeText={(value) => {
                setUsername(value);
                if (error) {
                  setError(null);
                }
              }}
              autoCorrect={false}
              autoCapitalize="none"
              editable={!submitting}
              error={error ?? undefined}
            />
          </View>

          <View style={styles.actions}>
            <Pressable
              onPress={onCancel}
              disabled={submitting}
              style={({ pressed }) => [
                styles.secondaryButton,
                pressed && !submitting && styles.pressed,
                submitting && styles.disabled,
              ]}
            >
              <Text style={styles.secondaryButtonText}>Cancel</Text>
            </Pressable>

            <Pressable
              onPress={() => {
                void handleSubmit();
              }}
              disabled={submitting}
              style={({ pressed }) => [
                styles.primaryButton,
                pressed && !submitting && styles.pressed,
                submitting && styles.disabled,
              ]}
            >
              {submitting ? (
                <ActivityIndicator color="#ffffff" />
              ) : (
                <Text style={styles.primaryButtonText}>Send request</Text>
              )}
            </Pressable>
          </View>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
};

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(15, 23, 42, 0.45)',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  sheet: {
    width: '100%',
    backgroundColor: '#ffffff',
    borderRadius: 18,
    padding: 20,
  },
  title: {
    fontSize: 18,
    fontWeight: '800',
    color: '#0f172a',
  },
  subtitle: {
    marginTop: 6,
    fontSize: 13,
    color: '#64748b',
  },
  inputWrapper: {
    marginTop: 16,
  },
  actions: {
    marginTop: 4,
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: 10,
  },
  primaryButton: {
    minWidth: 130,
    height: 44,
    paddingHorizontal: 18,
    borderRadius: 10,
    backgroundColor: '#0f766e',
    alignItems: 'center',
    justifyContent: 'center',
  },
  primaryButtonText: {
    color: '#ffffff',
    fontWeight: '700',
    fontSize: 15,
  },
  secondaryButton: {
    height: 44,
    paddingHorizontal: 18,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  secondaryButtonText: {
    color: '#334155',
    fontWeight: '700',
    fontSize: 15,
  },
  pressed: {
    opacity: 0.85,
  },
  disabled: {
    opacity: 0.6,
  },
});

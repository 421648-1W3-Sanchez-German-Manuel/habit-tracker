import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { FriendsScreen } from '../screens/FriendsScreen';
import type { FriendsStackParamList } from '../types/navigation';

const Stack = createNativeStackNavigator<FriendsStackParamList>();

export const FriendsNavigator = () => {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: '#ffffff' },
        headerTintColor: '#0f172a',
        headerTitleAlign: 'center',
      }}
    >
      <Stack.Screen name="FriendsHome" component={FriendsScreen} options={{ headerShown: false }} />
    </Stack.Navigator>
  );
};

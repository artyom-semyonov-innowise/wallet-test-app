import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  Alert,
  ActivityIndicator,
} from 'react-native';
import walletService from '../wallet';
import mockBackendService from '../services/mockBackend';
import type { Transaction } from '../types';

const WalletApp: React.FC = () => {
  const [publicKey, setPublicKey] = useState<string | null>(null);
  const [keyExists, setKeyExists] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);

  const [amount, setAmount] = useState<string>('100.50');
  const [currency, setCurrency] = useState<string>('USD');
  const [nonce, setNonce] = useState<number>(1);

  const [status, setStatus] = useState<string>('');
  const [lastTransactionId, setLastTransactionId] = useState<string | null>(
    null,
  );

  useEffect(() => {
    checkKeyExists();
  }, []);

  const checkKeyExists = async () => {
    try {
      const exists = await walletService.keyExists();
      setKeyExists(exists);
      if (exists) {
        const pubKey = await walletService.getPublicKey();
        setPublicKey(pubKey);
        const nextNonce = await walletService.getNextNonce();
        setNonce(nextNonce);
      }
    } catch (error: any) {
      setStatus(`Error checking key: ${error.message}`);
    }
  };

  const handleGenerateKey = async () => {
    setLoading(true);
    setStatus('');

    try {
      const result = await walletService.generateKeyPair();
      setPublicKey(result.publicKey);
      setKeyExists(true);
      const nextNonce = await walletService.getNextNonce();
      setNonce(nextNonce);
      setStatus('Key pair generated successfully!');
      Alert.alert('Success', 'Key pair generated successfully');
    } catch (error: any) {
      const errorMessage = `Failed to generate key: ${error.message}`;
      setStatus(errorMessage);
      Alert.alert('Error', errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleSignAndSend = async () => {
    if (!keyExists) {
      Alert.alert('Error', 'Please generate a key pair first');
      return;
    }

    if (!amount || !currency || !nonce) {
      Alert.alert('Error', 'Please fill in all transaction fields');
      return;
    }

    setLoading(true);
    setStatus('');
    setLastTransactionId(null);

    try {
      const transaction: Transaction = {
        amount,
        currency,
        nonce,
      };

      const signature = await walletService.signTransaction(transaction);

      if (!publicKey) {
        throw new Error('Public key not available');
      }

      const response = await mockBackendService.submitTransaction({
        transaction,
        signature,
        publicKey,
      });

      if (response.success) {
        setStatus(`Transaction successful! ID: ${response.transactionId}`);
        setLastTransactionId(response.transactionId || null);

        const nextNonce = await walletService.getNextNonce();
        setNonce(nextNonce);
        Alert.alert(
          'Success',
          `Transaction submitted successfully!\nID: ${response.transactionId}`,
        );
      } else {
        setStatus(`Transaction failed: ${response.error || response.message}`);
        Alert.alert('Error', response.error || 'Transaction failed');
      }
    } catch (error: any) {
      const errorMessage = `Failed to sign/send transaction: ${error.message}`;
      setStatus(errorMessage);
      Alert.alert('Error', errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.title}>Mobile Wallet</Text>
      <Text style={styles.subtitle}>Secure Transaction Signing</Text>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Key Status</Text>
        <Text style={styles.info}>
          Key Pair: {keyExists ? '✓ Generated' : '✗ Not generated'}
        </Text>

        {!keyExists && (
          <TouchableOpacity
            style={[styles.button, styles.primaryButton]}
            onPress={handleGenerateKey}
            disabled={loading}
          >
            {loading ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <Text style={styles.buttonText}>Generate Key Pair</Text>
            )}
          </TouchableOpacity>
        )}

        {publicKey && (
          <View style={styles.publicKeyContainer}>
            <Text style={styles.label}>Public Key:</Text>
            <Text
              style={styles.publicKey}
              numberOfLines={2}
              ellipsizeMode="middle"
            >
              {publicKey}
            </Text>
          </View>
        )}
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Create Transaction</Text>

        <Text style={styles.label}>Amount</Text>
        <TextInput
          style={styles.input}
          value={amount}
          onChangeText={setAmount}
          placeholder="100.50"
          keyboardType="decimal-pad"
        />

        <Text style={styles.label}>Currency</Text>
        <TextInput
          style={styles.input}
          value={currency}
          onChangeText={setCurrency}
            placeholder="USD"
          />

        <Text style={styles.label}>Nonce</Text>
        <TextInput
          style={[styles.input, styles.inputReadonly]}
          value={nonce.toString()}
          editable={false}
          placeholder="1"
          keyboardType="number-pad"
        />

        <TouchableOpacity
          style={[
            styles.button,
            styles.primaryButton,
            (!keyExists || loading) && styles.buttonDisabled,
          ]}
          onPress={handleSignAndSend}
          disabled={!keyExists || loading}
        >
          {loading ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.buttonText}>Sign & Send Transaction</Text>
          )}
        </TouchableOpacity>
      </View>

      {status && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Status</Text>
          <Text
            style={[
              styles.status,
              status.includes('successful') && styles.statusSuccess,
            ]}
          >
            {status}
          </Text>
        </View>
      )}

      {lastTransactionId && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Last Transaction</Text>
          <Text style={styles.transactionId}>{lastTransactionId}</Text>
        </View>
      )}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  content: {
    padding: 20,
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 8,
    color: '#333',
  },
  subtitle: {
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 24,
    color: '#666',
  },
  section: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 8,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 2,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 12,
    color: '#333',
  },
  label: {
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 8,
    color: '#333',
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 6,
    padding: 12,
    fontSize: 16,
    marginBottom: 16,
    backgroundColor: '#fafafa',
  },
  inputReadonly: {
    backgroundColor: '#f0f0f0',
    color: '#666',
  },
  button: {
    padding: 14,
    borderRadius: 6,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 8,
  },
  primaryButton: {
    backgroundColor: '#007AFF',
  },
  buttonDisabled: {
    backgroundColor: '#ccc',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  info: {
    fontSize: 14,
    color: '#666',
    marginBottom: 8,
  },
  publicKeyContainer: {
    marginTop: 12,
    padding: 12,
    backgroundColor: '#f9f9f9',
    borderRadius: 6,
  },
  publicKey: {
    fontSize: 12,
    fontFamily: 'monospace',
    color: '#333',
    marginTop: 4,
  },
  status: {
    fontSize: 14,
    color: '#d32f2f',
    marginTop: 8,
  },
  statusSuccess: {
    color: '#2e7d32',
  },
  transactionId: {
    fontSize: 14,
    fontFamily: 'monospace',
    color: '#007AFF',
    marginTop: 8,
  },
});

export default WalletApp;

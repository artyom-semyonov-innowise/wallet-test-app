import { NativeModules } from 'react-native';
import ReactNativeBiometrics from 'react-native-biometrics';
import type { Transaction, GenerateKeyPairResult, WalletError } from './types';

const { WalletModule } = NativeModules;

if (!WalletModule) {
  throw new Error(
    'WalletModule is not available. Make sure the native module is properly linked.',
  );
}
class WalletService {
  async generateKeyPair(): Promise<GenerateKeyPairResult> {
    try {
      const rnBiometrics = new ReactNativeBiometrics();

      const promptMessage = 'Authenticate to generate key pair';
      const { success } = await rnBiometrics.simplePrompt({
        promptMessage,
        fallbackPromptMessage: 'Use device PIN or password',
        cancelButtonText: 'Cancel',
      });

      if (!success) {
        throw new Error('Authentication cancelled or failed');
      }

      return await WalletModule.generateKeyPair();
    } catch (error: any) {
      throw this.createWalletError(error);
    }
  }

  async getPublicKey(): Promise<string> {
    try {
      return await WalletModule.getPublicKey();
    } catch (error: any) {
      throw this.createWalletError(error);
    }
  }

  async signTransaction(transaction: Transaction): Promise<string> {
    try {
      if (
        !transaction.amount ||
        !transaction.currency ||
        typeof transaction.nonce !== 'number'
      ) {
        throw new Error(
          'Transaction must have amount (string), currency (string), and nonce (number)',
        );
      }

      const amountValue = parseFloat(transaction.amount);
      if (isNaN(amountValue) || amountValue <= 0) {
        throw new Error('Amount must be a positive number');
      }

      const decimalPlaces = (transaction.amount.split('.')[1] || '').length;
      if (decimalPlaces > 18) {
        throw new Error('Amount cannot have more than 18 decimal places');
      }

      const rnBiometrics = new ReactNativeBiometrics();

      const promptMessage = `Authenticate to sign transaction\nAmount: ${transaction.amount} ${transaction.currency}`;
      const { success } = await rnBiometrics.simplePrompt({
        promptMessage,
        fallbackPromptMessage: 'Use device PIN or password',
        cancelButtonText: 'Cancel',
      });

      if (!success) {
        throw new Error('Authentication cancelled or failed');
      }

      return await WalletModule.signTransaction({
        amount: transaction.amount,
        currency: transaction.currency,
        nonce: transaction.nonce,
      });
    } catch (error: any) {
      throw this.createWalletError(error);
    }
  }

  async keyExists(): Promise<boolean> {
    try {
      return await WalletModule.keyExists();
    } catch (error: any) {
      throw this.createWalletError(error);
    }
  }

  async getNextNonce(): Promise<number> {
    try {
      const nextNonce = await WalletModule.getNextNonce();
      return Math.floor(nextNonce);
    } catch (error: any) {
      throw this.createWalletError(error);
    }
  }

  private createWalletError(error: any): WalletError {
    const walletError = new Error(
      error.message || 'Unknown wallet error',
    ) as WalletError;
    walletError.code = error.code || 'UNKNOWN_ERROR';
    return walletError;
  }
}

export const walletService = new WalletService();
export default walletService;

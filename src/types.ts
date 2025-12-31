export interface Transaction {
  amount: string;
  currency: string;
  nonce: number;
}

export interface GenerateKeyPairResult {
  publicKey: string;
}

export type WalletErrorCode =
  | 'WALLET_ERROR'
  | 'INVALID_INPUT'
  | 'UNKNOWN_ERROR';

export interface WalletError extends Error {
  code?: WalletErrorCode;
}

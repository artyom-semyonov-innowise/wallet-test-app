import type { Transaction } from '../types';

export interface TransactionRequest {
  transaction: Transaction;
  signature: string;
  publicKey: string;
}

export interface TransactionResponse {
  success: boolean;
  transactionId?: string;
  message?: string;
  error?: string;
}

const processedNonces = new Set<number>();

class MockBackendService {
  async submitTransaction(
    request: TransactionRequest,
  ): Promise<TransactionResponse> {
    await new Promise(resolve => setTimeout(resolve, 500));

    try {
      const validationResult = this.validateTransaction(request);

      if (!validationResult.valid) {
        return {
          success: false,
          error: validationResult.error,
        };
      }

      processedNonces.add(request.transaction.nonce);

      const transactionId = `tx_${Date.now()}_${Math.random()
        .toString(36)
        .substr(2, 9)}`;

      return {
        success: true,
        transactionId,
        message: 'Transaction processed successfully',
      };
    } catch (error: any) {
      return {
        success: false,
        error: error.message || 'Failed to process transaction',
      };
    }
  }

  private validateTransaction(request: TransactionRequest): {
    valid: boolean;
    error?: string;
  } {
    const { transaction, signature, publicKey } = request;

    if (!transaction.amount || !transaction.currency || typeof transaction.nonce !== 'number') {
      return {
        valid: false,
        error: 'Invalid transaction structure',
      };
    }

    const amount = parseFloat(transaction.amount);
    if (isNaN(amount) || amount <= 0) {
      return {
        valid: false,
        error: 'Amount must be a positive number',
      };
    }

    if (processedNonces.has(transaction.nonce)) {
      return {
        valid: false,
        error: `Nonce ${transaction.nonce} has already been used (replay attack detected)`,
      };
    }

    if (!signature || signature.length === 0) {
      return {
        valid: false,
        error: 'Signature is required',
      };
    }

    if (!publicKey || publicKey.length === 0) {
      return {
        valid: false,
        error: 'Public key is required',
      };
    }

    if (signature.trim().length === 0) {
      return {
        valid: false,
        error: 'Signature cannot be empty',
      };
    }

    return { valid: true };
  }
  clearProcessedNonces(): void {
    processedNonces.clear();
  }
  getProcessedNonces(): number[] {
    return Array.from(processedNonces);
  }
}

export const mockBackendService = new MockBackendService();
export default mockBackendService;

package im.mak.waves.transactions;

import im.mak.waves.crypto.account.PublicKey;
import im.mak.waves.transactions.common.Amount;
import im.mak.waves.transactions.common.Asset;
import im.mak.waves.transactions.common.Proof;
import im.mak.waves.transactions.common.Recipient;
import im.mak.waves.transactions.components.invoke.Function;

import java.io.IOException;
import java.util.*;

public class InvokeScriptTransaction extends Transaction {

    public static final int TYPE = 16;
    public static final int LATEST_VERSION = 2;
    public static final long MIN_FEE = 500_000;

    private final Recipient dApp;
    private final Function function;
    private final List<Amount> payments;

    public InvokeScriptTransaction(PublicKey sender, Recipient dApp, Function function, List<Amount> payments,
                                   byte chainId, long fee, Asset feeAsset, long timestamp, int version) {
        this(sender, dApp, function, payments, chainId, fee, feeAsset, timestamp, version, Proof.emptyList());
    }

    public InvokeScriptTransaction(PublicKey sender, Recipient dApp, Function function, List<Amount> payments,
                                   byte chainId, long fee, Asset feeAsset, long timestamp, int version, List<Proof> proofs) {
        super(TYPE, version, chainId, sender, fee, feeAsset, timestamp, proofs);
        if (dApp == null)
            throw new IllegalArgumentException("dApp can't be null");

        this.dApp = dApp;
        this.function = function == null ? Function.asDefault() : function;
        this.payments = payments == null ? Collections.emptyList() : payments;
    }

    public static InvokeScriptTransaction fromBytes(byte[] bytes) throws IOException {
        return (InvokeScriptTransaction) Transaction.fromBytes(bytes);
    }

    public static InvokeScriptTransaction fromJson(String json) throws IOException {
        return (InvokeScriptTransaction) Transaction.fromJson(json);
    }

    public static InvokeScriptTransactionBuilder with(Recipient dApp, Function function) {
        return new InvokeScriptTransactionBuilder(dApp, function);
    }

    public Recipient dApp() {
        return dApp;
    }

    public Function function() {
        return function;
    }

    public List<Amount> payments() {
        return payments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InvokeScriptTransaction that = (InvokeScriptTransaction) o;
        return this.dApp.equals(that.dApp)
                && this.function.equals(that.function)
                && this.payments.equals(that.payments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dApp, function, payments);
    }

    public static class InvokeScriptTransactionBuilder
            extends TransactionBuilder<InvokeScriptTransactionBuilder, InvokeScriptTransaction> {
        private final Recipient dApp;
        private final Function function;
        private final List<Amount> payments;

        protected InvokeScriptTransactionBuilder(Recipient dApp, Function function) {
            super(LATEST_VERSION, MIN_FEE);
            this.dApp = dApp;
            this.function = function;
            this.payments = new ArrayList<>();
        }

        public InvokeScriptTransactionBuilder payments(Amount... payments) {
            payments(Arrays.asList(payments));
            return this;
        }

        public InvokeScriptTransactionBuilder payments(List<Amount> payments) {
            this.payments.addAll(payments);
            return this;
        }

        protected InvokeScriptTransaction _build() {
            return new InvokeScriptTransaction(sender, dApp, function, payments, chainId, fee, feeAsset, timestamp, version);
        }
    }

}

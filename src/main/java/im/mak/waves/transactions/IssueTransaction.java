package im.mak.waves.transactions;

import im.mak.waves.crypto.Bytes;
import im.mak.waves.crypto.account.PublicKey;
import im.mak.waves.crypto.base.Base64;
import im.mak.waves.transactions.common.Asset;
import im.mak.waves.transactions.common.Proof;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

public class IssueTransaction extends Transaction {

    //todo checkstyle custom checks
    public static final int TYPE = 3;
    public static final int LATEST_VERSION = 3;
    public static final long MIN_FEE = 100_000_000;

    private final byte[] name;
    private final byte[] description;
    private final long quantity;
    private final int decimals;
    private final boolean isReissuable;
    private final byte[] compiledScript;

    public IssueTransaction(PublicKey sender, String name, String description, long quantity, int decimals,
                            boolean isReissuable, byte[] compiledScript, byte chainId, long fee, long timestamp, int version) {
        this(sender, name, description, quantity, decimals, isReissuable, compiledScript, chainId, fee, timestamp, version, Proof.emptyList());
    }

    public IssueTransaction(PublicKey sender, String name, String description, long quantity, int decimals,
                            boolean isReissuable, byte[] compiledScript, byte chainId, long fee, long timestamp,
                            int version, List<Proof> proofs) {
        this(sender, name == null ? Bytes.empty() : name.getBytes(UTF_8),
                description == null ? Bytes.empty() : description.getBytes(UTF_8), quantity, decimals, isReissuable,
                compiledScript, chainId, fee, timestamp, version, proofs);
    }

    public IssueTransaction(PublicKey sender, byte[] name, byte[] description, long quantity, int decimals,
                            boolean isReissuable, byte[] compiledScript, byte chainId, long fee, long timestamp,
                            int version, List<Proof> proofs) {
        super(TYPE, version, chainId, sender, fee, Asset.WAVES, timestamp, proofs);

        this.name = name == null ? Bytes.empty() : name;
        this.description = description == null ? Bytes.empty() : description;
        this.quantity = quantity;
        this.decimals = decimals;
        this.isReissuable = isReissuable;
        this.compiledScript = compiledScript == null ? Bytes.empty() : compiledScript;
    }

    public static IssueTransaction fromBytes(byte[] bytes) throws IOException {
        return (IssueTransaction) Transaction.fromBytes(bytes);
    }

    public static IssueTransaction fromJson(String json) throws IOException {
        return (IssueTransaction) Transaction.fromJson(json);
    }

    public static IssueTransaction.IssueTransactionBuilder with(String name, long quantity, int decimals) {
        return new IssueTransaction.IssueTransactionBuilder(name, quantity, decimals);
    }

    public static IssueTransaction.IssueTransactionBuilder with(byte[] name, long quantity, int decimals) {
        return new IssueTransaction.IssueTransactionBuilder(name, quantity, decimals);
    }

    public String name() {
        return new String(name, UTF_8);
    }

    public byte[] nameBytes() {
        return name;
    }

    public String description() {
        return new String(description, UTF_8);
    }

    public byte[] descriptionBytes() {
        return description;
    }

    public long quantity() {
        return quantity;
    }

    public int decimals() {
        return decimals;
    }

    public boolean isReissuable() {
        return isReissuable;
    }

    public byte[] compiledScript() {
        return compiledScript;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        IssueTransaction that = (IssueTransaction) o;
        return Bytes.equal(this.name, that.name)
                && Bytes.equal(this.description, that.description)
                && this.quantity == that.quantity
                && this.decimals == that.decimals
                && this.isReissuable == that.isReissuable
                && this.compiledScript == that.compiledScript;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, description, quantity, decimals, isReissuable, compiledScript);
    }

    public static class IssueTransactionBuilder
            extends TransactionBuilder<IssueTransaction.IssueTransactionBuilder, IssueTransaction> {
        private final byte[] name;
        private byte[] description;
        private final long quantity;
        private final int decimals;
        private boolean isReissuable;
        private byte[] compiledScript;

        protected IssueTransactionBuilder(String name, long quantity, int decimals) {
            this(name == null ? Bytes.empty() : name.getBytes(UTF_8), quantity, decimals);
        }

        protected IssueTransactionBuilder(byte[] name, long quantity, int decimals) {
            super(LATEST_VERSION, MIN_FEE);
            this.name = name;
            this.quantity = quantity;
            this.decimals = decimals;
            this.description = Bytes.empty();
            this.isReissuable = true;
            this.compiledScript = Bytes.empty();
        }

        public IssueTransactionBuilder description(String description) {
            this.description = description.getBytes(UTF_8);
            return this;
        }

        public IssueTransactionBuilder description(byte[] description) {
            this.description = description;
            return this;
        }

        public IssueTransactionBuilder isReissuable(boolean isReissuable) {
            this.isReissuable = isReissuable;
            return this;
        }

        public IssueTransactionBuilder compiledScript(String compiledBase64Script) {
            return compiledScript(compiledBase64Script == null ? null : Base64.decode(compiledBase64Script));
        }

        public IssueTransactionBuilder compiledScript(byte[] compiledScript) {
            this.compiledScript = compiledScript;
            return this;
        }

        protected IssueTransaction _build() {
            return new IssueTransaction(sender, name, description, quantity, decimals, isReissuable, compiledScript,
                    chainId, fee, timestamp, version, Proof.emptyList());
        }
    }
    
}
package im.mak.waves.transactions;

import im.mak.waves.crypto.account.PublicKey;
import im.mak.waves.transactions.common.Asset;
import im.mak.waves.transactions.common.Proof;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class UpdateAssetInfoTransaction extends Transaction {

    public static final int TYPE = 17;
    public static final int LATEST_VERSION = 1;
    public static final long MIN_FEE = 100_000;

    private final Asset asset;
    private final String name;
    private final String description;

    public UpdateAssetInfoTransaction(PublicKey sender, Asset asset, String name, String description, byte chainId, long fee, long timestamp, int version) {
        this(sender, asset, name, description, chainId, fee, timestamp, version, Proof.emptyList());
    }

    public UpdateAssetInfoTransaction(PublicKey sender, Asset asset, String name, String description, byte chainId, long fee, long timestamp, int version, List<Proof> proofs) {
        super(TYPE, version, chainId, sender, fee, Asset.WAVES, timestamp, proofs);
        if (asset.isWaves())
            throw new IllegalArgumentException("Can't be Waves");

        this.asset = asset;
        this.name = name == null ? "" : name;
        this.description = description == null ? "" : description;
    }

    public static UpdateAssetInfoTransaction fromBytes(byte[] bytes) throws IOException {
        return (UpdateAssetInfoTransaction) Transaction.fromBytes(bytes);
    }

    public static UpdateAssetInfoTransaction fromJson(String json) throws IOException {
        return (UpdateAssetInfoTransaction) Transaction.fromJson(json);
    }

    public static UpdateAssetInfoTransactionBuilder with(Asset asset, String name, String description) {
        return new UpdateAssetInfoTransactionBuilder(asset, name, description);
    }

    public Asset asset() {
        return asset;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UpdateAssetInfoTransaction that = (UpdateAssetInfoTransaction) o;
        return this.asset.equals(that.asset)
                && this.name.equals(that.name)
                && this.description.equals(that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), asset, name, description);
    }

    public static class UpdateAssetInfoTransactionBuilder
            extends TransactionBuilder<UpdateAssetInfoTransactionBuilder, UpdateAssetInfoTransaction> {
        private final Asset asset;
        private final String name;
        private final String description;

        protected UpdateAssetInfoTransactionBuilder(Asset asset, String name, String description) {
            super(LATEST_VERSION, MIN_FEE);
            this.asset = asset;
            this.name = name;
            this.description = description;
        }

        protected UpdateAssetInfoTransaction _build() {
            return new UpdateAssetInfoTransaction(sender, asset, name, description, chainId, fee, timestamp, version, Proof.emptyList());
        }
    }
    
}
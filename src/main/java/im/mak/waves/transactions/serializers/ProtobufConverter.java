package im.mak.waves.transactions.serializers;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.wavesplatform.protobuf.AmountOuterClass;
import com.wavesplatform.protobuf.order.OrderOuterClass;
import com.wavesplatform.protobuf.transaction.RecipientOuterClass;
import com.wavesplatform.protobuf.transaction.TransactionOuterClass;
import im.mak.waves.crypto.account.Address;
import im.mak.waves.crypto.account.PublicKey;
import im.mak.waves.transactions.*;
import im.mak.waves.transactions.common.*;
import im.mak.waves.transactions.data.*;
import im.mak.waves.transactions.exchange.Order;
import im.mak.waves.transactions.exchange.OrderType;
import im.mak.waves.transactions.mass.Transfer;
import im.mak.waves.transactions.invocation.Function;

import java.io.IOException;
import java.util.List;

import static com.wavesplatform.protobuf.transaction.TransactionOuterClass.DataTransactionData.DataEntry.ValueCase.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

public abstract class ProtobufConverter {

    public static Order fromProtobuf(OrderOuterClass.Order pbOrder) throws IOException {
        OrderType type;
        if (pbOrder.getOrderSide() == OrderOuterClass.Order.Side.BUY)
            type = OrderType.BUY;
        else if (pbOrder.getOrderSide() == OrderOuterClass.Order.Side.SELL)
            type = OrderType.SELL;
        else throw new IOException("Unknown order type \"" + pbOrder.getOrderSide() + "\"");

        Order order = Order
                .with(type,
                        Amount.of(pbOrder.getAmount(), AssetId.as(pbOrder.getAssetPair().getAmountAssetId().toByteArray())),
                        Amount.of(pbOrder.getPrice(), AssetId.as(pbOrder.getAssetPair().getPriceAssetId().toByteArray())),
                        PublicKey.as(pbOrder.getMatcherPublicKey().toByteArray()))
                .version(pbOrder.getVersion())
                .chainId((byte) pbOrder.getChainId())
                .sender(PublicKey.as(pbOrder.getSenderPublicKey().toByteArray()))
                .fee(pbAmountToAmount(pbOrder.getMatcherFee()))
                .timestamp(pbOrder.getTimestamp())
                .expiration(pbOrder.getExpiration())
                .get();
        pbOrder.getProofsList().forEach(p -> order.proofs().add(Proof.as(p.toByteArray())));
        return order;
    }

    public static Transaction fromProtobuf(TransactionOuterClass.SignedTransaction pbSignedTx) throws IOException {
        Transaction tx;
        TransactionOuterClass.Transaction pbTx = pbSignedTx.getTransaction();

        if (pbTx.hasGenesis()) {
            TransactionOuterClass.GenesisTransactionData genesis = pbTx.getGenesis();
            tx = new GenesisTransaction(
                    Address.as(genesis.getRecipientAddress().toByteArray()),
                    genesis.getAmount(),
                    pbTx.getTimestamp(),
                    pbSignedTx.getProofsCount() > 0 ? Proof.as(pbSignedTx.getProofs(0).toByteArray()) : Proof.EMPTY
            );
        } else if (pbTx.hasPayment()) {
            TransactionOuterClass.PaymentTransactionData payment = pbTx.getPayment();
            tx = new PaymentTransaction(
                    PublicKey.as(pbTx.getSenderPublicKey().toByteArray()),
                    Address.as(payment.getRecipientAddress().toByteArray()),
                    payment.getAmount(),
                    pbAmountToAmount(pbTx.getFee()),
                    pbTx.getTimestamp(),
                    Proof.EMPTY
            );
        } else if (pbTx.hasIssue()) {
            TransactionOuterClass.IssueTransactionData issue = pbTx.getIssue();
            tx = new IssueTransaction(
                    PublicKey.as(pbTx.getSenderPublicKey().toByteArray()),
                    issue.getNameBytes().toByteArray(),
                    issue.getDescriptionBytes().toByteArray(),
                    issue.getAmount(),
                    issue.getDecimals(),
                    issue.getReissuable(),
                    issue.getScript().toByteArray(),
                    (byte) pbTx.getChainId(),
                    pbAmountToAmount(pbTx.getFee()),
                    pbTx.getTimestamp(),
                    pbTx.getVersion(),
                    Proof.emptyList()
            );
        } else if (pbTx.hasTransfer()) {
            TransactionOuterClass.TransferTransactionData transfer = pbTx.getTransfer();
            AmountOuterClass.Amount amount = transfer.getAmount();
            tx = TransferTransaction
                    .with(recipientFromProto(transfer.getRecipient(), (byte) pbTx.getChainId()),
                            Amount.of(amount.getAmount(), AssetId.as(amount.getAssetId().toByteArray())))
                    .attachment(transfer.getAttachment().toByteArray())
                    .version(pbTx.getVersion())
                    .chainId((byte) pbTx.getChainId())
                    .sender(PublicKey.as(pbTx.getSenderPublicKey().toByteArray()))
                    .fee(pbAmountToAmount(pbTx.getFee()))
                    .timestamp(pbTx.getTimestamp())
                    .get();
        } else if (pbTx.hasReissue()) {
            TransactionOuterClass.ReissueTransactionData reissue = pbTx.getReissue();
            tx = ReissueTransaction
                    .with(Amount.of(reissue.getAssetAmount().getAmount(), //todo why asset_amount??
                            AssetId.as(reissue.getAssetAmount().getAssetId().toByteArray())))
                    .reissuable(reissue.getReissuable())
                    .version(pbTx.getVersion())
                    .chainId((byte) pbTx.getChainId())
                    .sender(PublicKey.as(pbTx.getSenderPublicKey().toByteArray()))
                    .fee(pbAmountToAmount(pbTx.getFee()))
                    .timestamp(pbTx.getTimestamp())
                    .get();
        } else if (pbTx.hasBurn()) {
            TransactionOuterClass.BurnTransactionData burn = pbTx.getBurn();
            tx = BurnTransaction
                    .with(Amount.of(burn.getAssetAmount().getAmount(),
                            AssetId.as(burn.getAssetAmount().getAssetId().toByteArray())))
                    .version(pbTx.getVersion())
                    .chainId((byte) pbTx.getChainId())
                    .sender(PublicKey.as(pbTx.getSenderPublicKey().toByteArray()))
                    .fee(pbAmountToAmount(pbTx.getFee()))
                    .timestamp(pbTx.getTimestamp())
                    .get();
        } else if (pbTx.hasExchange()) {
            TransactionOuterClass.ExchangeTransactionData exchange = pbTx.getExchange();
            tx = ExchangeTransaction
                    .with(fromProtobuf(exchange.getOrders(0)), fromProtobuf(exchange.getOrders(1)))
                    .amount(exchange.getAmount())
                    .price(exchange.getPrice())
                    .buyMatcherFee(exchange.getBuyMatcherFee())
                    .sellMatcherFee(exchange.getSellMatcherFee())
                    .version(pbTx.getVersion())
                    .chainId((byte) pbTx.getChainId())
                    .sender(PublicKey.as(pbTx.getSenderPublicKey().toByteArray()))
                    .fee(pbAmountToAmount(pbTx.getFee()))
                    .timestamp(pbTx.getTimestamp())
                    .get();
        } else if (pbTx.hasLease()) {
            TransactionOuterClass.LeaseTransactionData lease = pbTx.getLease();
            tx = LeaseTransaction
                    .with(recipientFromProto(lease.getRecipient(), (byte) pbTx.getChainId()), lease.getAmount())
                    .version(pbTx.getVersion())
                    .chainId((byte) pbTx.getChainId())
                    .sender(PublicKey.as(pbTx.getSenderPublicKey().toByteArray()))
                    .fee(pbAmountToAmount(pbTx.getFee()))
                    .timestamp(pbTx.getTimestamp())
                    .get();
        } else if (pbTx.hasLeaseCancel()) {
            TransactionOuterClass.LeaseCancelTransactionData leaseCancel = pbTx.getLeaseCancel();
            tx = LeaseCancelTransaction
                    .with(Id.as(leaseCancel.getLeaseId().toByteArray()))
                    .version(pbTx.getVersion())
                    .chainId((byte) pbTx.getChainId())
                    .sender(PublicKey.as(pbTx.getSenderPublicKey().toByteArray()))
                    .fee(pbAmountToAmount(pbTx.getFee()))
                    .timestamp(pbTx.getTimestamp())
                    .get();
        } else if (pbTx.hasCreateAlias()) {
            TransactionOuterClass.CreateAliasTransactionData alias = pbTx.getCreateAlias();
            tx = CreateAliasTransaction
                    .with(new String(alias.getAliasBytes().toByteArray(), UTF_8)) //todo is there non utf8 aliases?
                    .version(pbTx.getVersion())
                    .chainId((byte) pbTx.getChainId())
                    .sender(PublicKey.as(pbTx.getSenderPublicKey().toByteArray()))
                    .fee(pbAmountToAmount(pbTx.getFee()))
                    .timestamp(pbTx.getTimestamp())
                    .get();
        } else if (pbTx.hasMassTransfer()) {
            TransactionOuterClass.MassTransferTransactionData massTransfer = pbTx.getMassTransfer();
            List<Transfer> transfers = massTransfer.getTransfersList()
                    .stream()
                    .map(t -> Transfer.to(recipientFromProto(t.getRecipient(), (byte) pbTx.getChainId()), t.getAmount()))
                    .collect(toList());
            tx = MassTransferTransaction
                    .with(transfers)
                    .assetId(AssetId.as(massTransfer.getAssetId().toByteArray()))
                    .attachment(massTransfer.getAttachment().toByteArray())
                    .version(pbTx.getVersion())
                    .chainId((byte) pbTx.getChainId())
                    .sender(PublicKey.as(pbTx.getSenderPublicKey().toByteArray()))
                    .fee(pbAmountToAmount(pbTx.getFee()))
                    .timestamp(pbTx.getTimestamp())
                    .get();
        } else if (pbTx.hasDataTransaction()) {
            TransactionOuterClass.DataTransactionData data = pbTx.getDataTransaction();
            tx = DataTransaction
                    .with(data.getDataList().stream().map(e -> {
                        if (e.getValueCase() == INT_VALUE)
                            return new IntegerEntry(e.getKey(), e.getIntValue());
                        else if (e.getValueCase() == BOOL_VALUE)
                            return new BooleanEntry(e.getKey(), e.getBoolValue());
                        else if (e.getValueCase() == BINARY_VALUE)
                            return new BinaryEntry(e.getKey(), e.getBinaryValue().toByteArray());
                        else if (e.getValueCase() == STRING_VALUE)
                            return new StringEntry(e.getKey(), e.getStringValue());
                        else if (e.getValueCase() == VALUE_NOT_SET)
                            return new DeleteEntry(e.getKey());
                        else throw new IllegalArgumentException("Unknown value case " + e.getValueCase().getNumber());
                    }).collect(toList()))
                    .version(pbTx.getVersion())
                    .chainId((byte) pbTx.getChainId())
                    .sender(PublicKey.as(pbTx.getSenderPublicKey().toByteArray()))
                    .fee(pbAmountToAmount(pbTx.getFee()))
                    .timestamp(pbTx.getTimestamp())
                    .get();
        } else if (pbTx.hasSetScript()) {
            TransactionOuterClass.SetScriptTransactionData setScript = pbTx.getSetScript();
            tx = SetScriptTransaction
                    .with(setScript.getScript().toByteArray())
                    .version(pbTx.getVersion())
                    .chainId((byte) pbTx.getChainId())
                    .sender(PublicKey.as(pbTx.getSenderPublicKey().toByteArray()))
                    .fee(pbAmountToAmount(pbTx.getFee()))
                    .timestamp(pbTx.getTimestamp())
                    .get();
        } else if (pbTx.hasSponsorFee()) {
            TransactionOuterClass.SponsorFeeTransactionData sponsor = pbTx.getSponsorFee();
            tx = SponsorFeeTransaction
                    .with(AssetId.as(sponsor.getMinFee().getAssetId().toByteArray()), sponsor.getMinFee().getAmount())
                    .version(pbTx.getVersion())
                    .chainId((byte) pbTx.getChainId())
                    .sender(PublicKey.as(pbTx.getSenderPublicKey().toByteArray()))
                    .fee(pbAmountToAmount(pbTx.getFee()))
                    .timestamp(pbTx.getTimestamp())
                    .get();
        } else if (pbTx.hasSetAssetScript()) {
            TransactionOuterClass.SetAssetScriptTransactionData setAssetScript = pbTx.getSetAssetScript();
            tx = SetAssetScriptTransaction
                    .with(AssetId.as(setAssetScript.getAssetId().toByteArray()), setAssetScript.getScript().toByteArray())
                    .version(pbTx.getVersion())
                    .chainId((byte) pbTx.getChainId())
                    .sender(PublicKey.as(pbTx.getSenderPublicKey().toByteArray()))
                    .fee(pbAmountToAmount(pbTx.getFee()))
                    .timestamp(pbTx.getTimestamp())
                    .get();
        } else if (pbTx.hasInvokeScript()) {
            TransactionOuterClass.InvokeScriptTransactionData invoke = pbTx.getInvokeScript();
            Function functionCall = new BytesReader(invoke.getFunctionCall().toByteArray()).readFunctionCall();
            tx = InvokeScriptTransaction
                    .with(recipientFromProto(invoke.getDApp(), (byte)pbTx.getChainId()), functionCall)
                    .payments(invoke.getPaymentsList().stream().map(p ->
                            Amount.of(p.getAmount(), AssetId.as(p.getAssetId().toByteArray())))
                            .collect(toList()))
                    .version(pbTx.getVersion())
                    .chainId((byte) pbTx.getChainId())
                    .sender(PublicKey.as(pbTx.getSenderPublicKey().toByteArray()))
                    .fee(pbAmountToAmount(pbTx.getFee()))
                    .timestamp(pbTx.getTimestamp())
                    .get();
        } else if (pbTx.hasUpdateAssetInfo()) {
            TransactionOuterClass.UpdateAssetInfoTransactionData update = pbTx.getUpdateAssetInfo();
            tx = UpdateAssetInfoTransaction
                    .with(AssetId.as(update.getAssetId().toByteArray()), update.getName(), update.getDescription())
                    .version(pbTx.getVersion())
                    .chainId((byte) pbTx.getChainId())
                    .sender(PublicKey.as(pbTx.getSenderPublicKey().toByteArray()))
                    .fee(pbAmountToAmount(pbTx.getFee()))
                    .timestamp(pbTx.getTimestamp())
                    .get();
        } else throw new InvalidProtocolBufferException("Can't recognize transaction type");

        pbSignedTx.getProofsList().forEach(p -> tx.proofs().add(Proof.as(p.toByteArray())));
        return tx;
    }

    public static OrderOuterClass.Order toUnsignedProtobuf(Order order) {
        OrderOuterClass.Order.Builder builder = OrderOuterClass.Order.newBuilder();
        if (order.type() == OrderType.BUY)
            builder.setOrderSide(OrderOuterClass.Order.Side.BUY);
        else if (order.type() == OrderType.SELL)
            builder.setOrderSide(OrderOuterClass.Order.Side.SELL);
        builder.setVersion(order.version())
                .setChainId(order.chainId())
                .setSenderPublicKey(ByteString.copyFrom(order.sender().bytes()))
                .setAssetPair(OrderOuterClass.AssetPair.newBuilder()
                        .setAmountAssetId(ByteString.copyFrom(order.amount().assetId().bytes()))
                        .setPriceAssetId(ByteString.copyFrom(order.price().assetId().bytes()))
                        .build())
                .setAmount(order.amount().value())
                .setPrice(order.price().value())
                .setMatcherPublicKey(ByteString.copyFrom(order.matcher().bytes()))
                .setMatcherFee(amountToPBAmount(order.fee()))
                .setTimestamp(order.timestamp())
                .setExpiration(order.expiration());
        return builder.build(); //todo bodyBytes are just without proofs?
    }

    public static TransactionOuterClass.Transaction toUnsignedProtobuf(Transaction tx) {
        TransactionOuterClass.Transaction.Builder protoBuilder = TransactionOuterClass.Transaction.newBuilder()
                .setVersion(tx.version())
                .setChainId(tx.chainId())
                .setSenderPublicKey(ByteString.copyFrom(tx.sender().bytes()))
                .setFee(amountToPBAmount(tx.fee()))
                .setTimestamp(tx.timestamp());

        if (tx instanceof GenesisTransaction) {
            GenesisTransaction gtx = (GenesisTransaction) tx;
            protoBuilder.setGenesis(TransactionOuterClass.GenesisTransactionData.newBuilder()
                    .setRecipientAddress(ByteString.copyFrom(gtx.recipient().bytes()))
                    .setAmount(gtx.amount())
                    .build());
        } else if (tx instanceof PaymentTransaction) {
            PaymentTransaction ptx = (PaymentTransaction) tx;
            protoBuilder.setPayment(TransactionOuterClass.PaymentTransactionData.newBuilder()
                    .setRecipientAddress(ByteString.copyFrom(ptx.recipient().bytes()))
                    .setAmount(ptx.amount())
                    .build());
        } else if (tx instanceof IssueTransaction) {
            IssueTransaction itx = (IssueTransaction) tx;
            protoBuilder.setIssue(TransactionOuterClass.IssueTransactionData.newBuilder()
                    .setNameBytes(ByteString.copyFrom(itx.nameBytes()))
                    .setDescriptionBytes(ByteString.copyFrom(itx.descriptionBytes()))
                    .setAmount(itx.quantity())
                    .setDecimals(itx.decimals())
                    .setReissuable(itx.isReissuable())
                    .setScript(ByteString.copyFrom(itx.compiledScript()))
                    .build());
        } else if (tx instanceof TransferTransaction) {
            TransferTransaction ttx = (TransferTransaction) tx;
            protoBuilder.setTransfer(TransactionOuterClass.TransferTransactionData.newBuilder()
                    .setRecipient(recipientToProto(ttx.recipient()))
                    .setAmount(AmountOuterClass.Amount.newBuilder()
                            .setAmount(ttx.amount().value())
                            .setAssetId(ByteString.copyFrom(ttx.amount().assetId().bytes()))
                            .build())
                    .setAttachment(ByteString.copyFrom(ttx.attachmentBytes()))
                    .build());
        } else if (tx instanceof ReissueTransaction) {
            ReissueTransaction rtx = (ReissueTransaction) tx;
            protoBuilder.setReissue(TransactionOuterClass.ReissueTransactionData.newBuilder()
                    .setAssetAmount(AmountOuterClass.Amount.newBuilder()
                            .setAssetId(ByteString.copyFrom(rtx.amount().assetId().bytes()))
                            .setAmount(rtx.amount().value())
                            .build())
                    .setReissuable(rtx.isReissuable())
                    .build());
        } else if (tx instanceof BurnTransaction) {
            BurnTransaction btx = (BurnTransaction) tx;
            protoBuilder.setBurn(TransactionOuterClass.BurnTransactionData.newBuilder()
                    .setAssetAmount(AmountOuterClass.Amount.newBuilder()
                            .setAssetId(ByteString.copyFrom(btx.amount().assetId().bytes()))
                            .setAmount(btx.amount().value())
                            .build())
                    .build());
        } else if (tx instanceof ExchangeTransaction) {
            ExchangeTransaction etx = (ExchangeTransaction) tx;
            OrderOuterClass.Order order1 = toProtobuf(etx.isDirectionBuySell() ? etx.buyOrder() : etx.sellOrder());
            OrderOuterClass.Order order2 = toProtobuf(etx.isDirectionBuySell() ? etx.sellOrder() : etx.buyOrder());
            protoBuilder.setExchange(TransactionOuterClass.ExchangeTransactionData.newBuilder()
                    .addOrders(order1)
                    .addOrders(order2)
                    .setAmount(etx.amount())
                    .setPrice(etx.price())
                    .setBuyMatcherFee(etx.buyMatcherFee())
                    .setSellMatcherFee(etx.sellMatcherFee())
                    .build());
        } else if (tx instanceof LeaseTransaction) {
            LeaseTransaction ltx = (LeaseTransaction) tx;
            protoBuilder.setLease(TransactionOuterClass.LeaseTransactionData.newBuilder()
                    .setRecipient(recipientToProto(ltx.recipient()))
                    .setAmount(ltx.amount())
                    .build());
        } else if (tx instanceof LeaseCancelTransaction) {
            LeaseCancelTransaction lctx = (LeaseCancelTransaction) tx;
            protoBuilder.setLeaseCancel(TransactionOuterClass.LeaseCancelTransactionData.newBuilder()
                    .setLeaseId(ByteString.copyFrom(lctx.leaseId().bytes()))
                    .build());
        } else if (tx instanceof CreateAliasTransaction) {
            CreateAliasTransaction caTx = (CreateAliasTransaction) tx;
            protoBuilder.setCreateAlias(TransactionOuterClass.CreateAliasTransactionData.newBuilder()
                    .setAliasBytes(ByteString.copyFrom(caTx.alias().getBytes(UTF_8))) //todo is there aliases with non utf8 values?
                    .build());
        } else if (tx instanceof MassTransferTransaction) {
            MassTransferTransaction mtTx = (MassTransferTransaction) tx;
            protoBuilder.setMassTransfer(TransactionOuterClass.MassTransferTransactionData.newBuilder()
                    .addAllTransfers(mtTx.transfers().stream().map(t ->
                        TransactionOuterClass.MassTransferTransactionData.Transfer.newBuilder()
                                .setRecipient(recipientToProto(t.recipient()))
                                .setAmount(t.amount())
                                .build()
                    ).collect(toList()))
                    .setAssetId(ByteString.copyFrom(mtTx.assetId().bytes()))
                    .setAttachment(ByteString.copyFrom(mtTx.attachmentBytes())))
                    .build();
        } else if (tx instanceof DataTransaction) {
            DataTransaction dtx = (DataTransaction) tx;
            protoBuilder.setDataTransaction(TransactionOuterClass.DataTransactionData.newBuilder()
                    .addAllData(dtx.data().stream().map(e -> {
                        TransactionOuterClass.DataTransactionData.DataEntry.Builder builder =
                                TransactionOuterClass.DataTransactionData.DataEntry.newBuilder().setKey(e.key());
                        if (e instanceof BinaryEntry) builder.setBinaryValue(ByteString.copyFrom(((BinaryEntry)e).value())).build();
                        else if (e instanceof BooleanEntry) builder.setBoolValue(((BooleanEntry)e).value()).build();
                        else if (e instanceof IntegerEntry) builder.setIntValue(((IntegerEntry)e).value()).build();
                        else if (e instanceof StringEntry) builder.setStringValue(((StringEntry)e).value()).build();
                        else if (e instanceof DeleteEntry) builder.setKey(e.key());
                        else throw new IllegalArgumentException("Unknown entry type " + e.type());
                        return builder.build();
                    }).collect(toList()))
                    .build());
        } else if (tx instanceof SetScriptTransaction) {
            SetScriptTransaction ssTx = (SetScriptTransaction) tx;
            protoBuilder.setSetScript(TransactionOuterClass.SetScriptTransactionData.newBuilder()
                    .setScript(ByteString.copyFrom(ssTx.compiledScript()))
                    .build());
        } else if (tx instanceof SponsorFeeTransaction) {
            SponsorFeeTransaction sfTx = (SponsorFeeTransaction) tx;
            protoBuilder.setSponsorFee(TransactionOuterClass.SponsorFeeTransactionData.newBuilder()
                    .setMinFee(AmountOuterClass.Amount.newBuilder()
                            .setAssetId(ByteString.copyFrom(sfTx.assetId().bytes()))
                            .setAmount(sfTx.minSponsoredFee())
                            .build())
                    .build());
        } else if (tx instanceof SetAssetScriptTransaction) {
            SetAssetScriptTransaction sasTx = (SetAssetScriptTransaction) tx;
            protoBuilder.setSetAssetScript(TransactionOuterClass.SetAssetScriptTransactionData.newBuilder()
                    .setAssetId(ByteString.copyFrom(sasTx.assetId().bytes()))
                    .setScript(ByteString.copyFrom(sasTx.compiledScript()))
                    .build());
        } else if (tx instanceof InvokeScriptTransaction) {
            InvokeScriptTransaction isTx = (InvokeScriptTransaction) tx;
            TransactionOuterClass.InvokeScriptTransactionData.Builder invoke =
                    TransactionOuterClass.InvokeScriptTransactionData.newBuilder();
            invoke.setDApp(recipientToProto(isTx.dApp()));
            invoke.setFunctionCall(ByteString.copyFrom(new BytesWriter().writeFunction(isTx.function()).getBytes()));
            isTx.payments().forEach(p -> invoke.addPayments(AmountOuterClass.Amount.newBuilder()
                    .setAmount(p.value())
                    .setAssetId(ByteString.copyFrom(p.assetId().bytes()))
                    .build()));
            protoBuilder.setInvokeScript(invoke.build());
        } else if (tx instanceof UpdateAssetInfoTransaction) {
            UpdateAssetInfoTransaction uaiTx = (UpdateAssetInfoTransaction) tx;
            protoBuilder.setUpdateAssetInfo(TransactionOuterClass.UpdateAssetInfoTransactionData.newBuilder()
                    .setAssetId(ByteString.copyFrom(uaiTx.assetId().bytes()))
                    .setName(uaiTx.name())
                    .setDescription(uaiTx.description())
                    .build());
        }

        return protoBuilder.build();
    }

    public static OrderOuterClass.Order toProtobuf(Order order) {
        return OrderOuterClass.Order.newBuilder(toUnsignedProtobuf(order))
                .addAllProofs(order.proofs()
                        .stream()
                        .map(p -> ByteString.copyFrom(p.bytes()))
                        .collect(toList()))
                .build();
    }

    public static TransactionOuterClass.SignedTransaction toProtobuf(Transaction tx) {
        return TransactionOuterClass.SignedTransaction.newBuilder()
                .setTransaction(toUnsignedProtobuf(tx))
                .addAllProofs(tx.proofs()
                        .stream()
                        .map(p -> ByteString.copyFrom(p.bytes()))
                        .collect(toList()))
                .build();
    }

    public static Recipient recipientFromProto(RecipientOuterClass.Recipient proto, byte chainId) {
        if (proto.getRecipientCase().getNumber() == 1)
            return Recipient.as(Address.fromPart(proto.getPublicKeyHash().toByteArray(), chainId));
        else if (proto.getRecipientCase().getNumber() == 2) {
            return Recipient.as(Alias.as(chainId, proto.getAlias()));
        } else throw new IllegalArgumentException("Protobuf recipient must be specified");
    }

    public static RecipientOuterClass.Recipient recipientToProto(Recipient recipient) {
        RecipientOuterClass.Recipient.Builder proto = RecipientOuterClass.Recipient.newBuilder();
        if (recipient.isAlias())
            proto.setAlias(recipient.alias().value());
        else
            proto.setPublicKeyHash(ByteString.copyFrom(
                    recipient.address().publicKeyHash()));
        return proto.build();
    }

    public static Amount pbAmountToAmount(AmountOuterClass.Amount amount) {
        return Amount.of(
                amount.getAmount(),
                AssetId.as(amount.getAssetId().toByteArray())
        );
    }

    public static AmountOuterClass.Amount amountToPBAmount(Amount amount) {
        return AmountOuterClass.Amount.newBuilder()
                .setAmount(amount.value())
                .setAssetId(ByteString.copyFrom(amount.assetId().bytes()))
                .build();
    }

}

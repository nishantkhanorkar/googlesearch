SpecificDatumReader<AccountTransactionEntity> reader = new SpecificDatumReader<>(
    AccountTransactionEntity.getClassSchema());
Decoder decoder = DecoderFactory.get().binaryDecoder(
    record.value(), null);
AccountTransactionEntity transaction = reader.read(null, decoder);

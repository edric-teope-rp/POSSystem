# Transaction Logging System

This directory contains all system logs for the POS System.

## Directory Structure

```
logs/
├── transactions/    # Virtual Journal (Transaction Log)
├── application/     # General application logs
├── errors/          # Error-only logs
└── README.md        # This file
```

## Transaction Journal Format

All transaction logs follow this pipe-delimited format:
```
YYYY-MM-DD HH:mm:ss.SSS|ACTION|DETAILS
```

**Note**: Transaction logs appear in this format on both:
- Console output (during application runtime)
- Log file: `logs/transactions/journal.log`

### Log Entry Types

#### Transaction Lifecycle
- `TX_CREATE|TX_ID:{id}`
- `TX_COMPLETE|TX_ID:{id}|TENDER:{type}|SUBTOTAL:{amount}|TAX:{amount}|TOTAL:{amount}|TENDERED:{amount}|CHANGE:{amount}`
- `TX_VOID|TX_ID:{id}|REASON:{reason}`

#### Item Operations
- `ITEM_ADD|TX_ID:{id}|ITEM_ID:{id}|UPC:{upc}|NAME:{name}|QTY:{qty}|UNIT_PRICE:{price}|LINE_TOTAL:{total}`
- `ITEM_QTY_UPDATE|TX_ID:{id}|ITEM_ID:{id}|UPC:{upc}|NAME:{name}|OLD_QTY:{qty}|NEW_QTY:{qty}|UNIT_PRICE:{price}|NEW_LINE_TOTAL:{total}`
- `ITEM_VOID|TX_ID:{id}|ITEM_ID:{id}|NAME:{name}|QTY:{qty}|VOIDED_AMOUNT:{amount}`

### Example Transaction Flow

```
2026-03-12 14:32:01.234|TX_CREATE|TX_ID:12345
2026-03-12 14:32:15.567|ITEM_ADD|TX_ID:12345|ITEM_ID:1|UPC:123456|NAME:Coca-Cola 12oz|QTY:1|UNIT_PRICE:1.99|LINE_TOTAL:1.99
2026-03-12 14:32:18.890|ITEM_QTY_UPDATE|TX_ID:12345|ITEM_ID:1|UPC:123456|NAME:Coca-Cola 12oz|OLD_QTY:1|NEW_QTY:2|UNIT_PRICE:1.99|NEW_LINE_TOTAL:3.98
2026-03-12 14:32:22.123|ITEM_ADD|TX_ID:12345|ITEM_ID:2|UPC:789012|NAME:Lay's Chips|QTY:1|UNIT_PRICE:3.49|LINE_TOTAL:3.49
2026-03-12 14:32:45.456|ITEM_VOID|TX_ID:12345|ITEM_ID:2|NAME:Lay's Chips|QTY:1|VOIDED_AMOUNT:3.49
2026-03-12 14:33:10.789|TX_COMPLETE|TX_ID:12345|TENDER:CASH|SUBTOTAL:3.98|TAX:0.28|TOTAL:4.26|TENDERED:5.00|CHANGE:0.74
```

## Retention Policy

- **Transaction Journal**: 10 years (120 months) - Required for tax/audit compliance
- **Application Logs**: 90 days
- **Error Logs**: 365 days

## Important Notes

⚠️ **NEVER DELETE TRANSACTION JOURNAL FILES** - These are legal records required for:
- Tax compliance
- Financial audits
- Dispute resolution
- Fraud investigation

## Log Files

Transaction journals are rolled monthly:
- Current: `transactions/journal.log`
- Archive: `transactions/journal-YYYY-MM.log`

Application logs are rolled daily and compressed:
- Current: `application/app.log`
- Archive: `application/app-YYYY-MM-DD.log.gz`

Error logs are rolled daily and compressed:
- Current: `errors/error.log`
- Archive: `errors/error-YYYY-MM-DD.log.gz`

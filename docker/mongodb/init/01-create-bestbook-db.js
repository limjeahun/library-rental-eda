// bestbook-service: MongoDB read model database and collection.
db = db.getSiblingDB("bestbook_db");

db.createCollection("best_books");

db.best_books.createIndex(
    { itemNo: 1 },
    {
        name: "uk_best_books_item_no",
        unique: true
    }
);

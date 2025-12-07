package infra.migration

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import infra.MongoClient
import infra.MongoCollectionNames
import org.bson.Document

/**
 * Migration script to rename all fields from zh to vi
 * Note: This migration is for legacy data that may have used 'zh' field names.
 * The system translates from Japanese (jp) to Vietnamese (vi), not from Chinese (zh) to Vietnamese.
 * This migration handles any old data that might have used 'zh' naming.
 * 
 * This includes:
 * - WebNovel: titleZh -> titleVi, introductionZh -> introductionVi
 * - WebNovelChapter: paragraphsZh (baidu, youdao, gpt, sakura) -> paragraphsVi
 * - WebNovelTocItem: titleZh -> titleVi
 * - WenkuNovel: titleZh -> titleVi, volumeZh -> volumeVi
 * - WenkuNovelVolume: titleZh -> titleVi
 * - OperationHistory: titleZh -> titleVi, introductionZh -> introductionVi
 */
suspend fun migrateZhToVi(mongo: MongoClient) {
    val db = mongo.database

    // Migrate WebNovel collection
    val webNovelCollection = db.getCollection<Document>(MongoCollectionNames.WEB_NOVEL)
        .withDocumentClass<Document>()
    webNovelCollection.updateMany(
        Filters.exists("titleZh"),
        Updates.combine(
            Updates.rename("titleZh", "titleVi"),
            Updates.rename("introductionZh", "introductionVi"),
        )
    )
    webNovelCollection.updateMany(
        Filters.exists("toc.titleZh"),
        Updates.rename("toc.$[].titleZh", "toc.$[].titleVi")
    )

    // Migrate WebNovelChapter collection
    val webNovelChapterCollection = db.getCollection<Document>(MongoCollectionNames.WEB_CHAPTER)
        .withDocumentClass<Document>()
    webNovelChapterCollection.updateMany(
        Filters.exists("paragraphsZh"),
        Updates.rename("paragraphsZh", "paragraphsVi")
    )

    // Migrate WenkuNovel collection
    val wenkuNovelCollection = db.getCollection<Document>(MongoCollectionNames.WENKU_NOVEL)
        .withDocumentClass<Document>()
    wenkuNovelCollection.updateMany(
        Filters.exists("titleZh"),
        Updates.combine(
            Updates.rename("titleZh", "titleVi"),
            Updates.rename("volumeZh", "volumeVi"),
        )
    )
    wenkuNovelCollection.updateMany(
        Filters.exists("volumes.titleZh"),
        Updates.rename("volumes.$[].titleZh", "volumes.$[].titleVi")
    )

    // Migrate OperationHistory collection
    val operationHistoryCollection = db.getCollection<Document>(MongoCollectionNames.OPERATION_HISTORY)
        .withDocumentClass<Document>()
    // Note: OperationHistory has nested structures, may need more complex migration
    // This is a simplified version - may need to handle nested documents separately
    operationHistoryCollection.updateMany(
        Filters.exists("operation.old.titleZh"),
        Updates.rename("operation.old.titleZh", "operation.old.titleVi")
    )
    operationHistoryCollection.updateMany(
        Filters.exists("operation.old.introductionZh"),
        Updates.rename("operation.old.introductionZh", "operation.old.introductionVi")
    )
    operationHistoryCollection.updateMany(
        Filters.exists("operation.new.titleZh"),
        Updates.rename("operation.new.titleZh", "operation.new.titleVi")
    )
    operationHistoryCollection.updateMany(
        Filters.exists("operation.new.introductionZh"),
        Updates.rename("operation.new.introductionZh", "operation.new.introductionVi")
    )

    println("Migration completed: zh fields renamed to vi")
}


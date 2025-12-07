package api

import api.model.WebNovelOutlineDto
import api.model.asDto
import api.plugins.*
import infra.common.*
import infra.oplog.Operation
import infra.oplog.OperationHistoryRepository
import infra.user.UserFavoredRepository
import infra.web.*
import infra.web.datasource.providers.Hameln
import infra.web.datasource.providers.Kakuyomu
import infra.web.datasource.providers.NovelIdShouldBeReplacedException
import infra.web.datasource.providers.Pixiv
import infra.web.datasource.providers.Syosetu
import infra.web.repository.*
import infra.wenku.repository.WenkuNovelMetadataRepository
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.resources.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import org.koin.ktor.ext.inject

@Resource("/novel")
private class WebNovelRes {
    @Resource("")
    class List(
        val parent: WebNovelRes,
        val page: Int,
        val pageSize: Int,
        val provider: String = "",
        val type: Int = 0,
        val level: Int = 0,
        val translate: Int = 0,
        val sort: Int = 0,
        val query: String? = null,
    )

    @Resource("/rank/{providerId}")
    class Rank(val parent: WebNovelRes, val providerId: String)

    @Resource("/{providerId}/{novelId}")
    class Id(val parent: WebNovelRes, val providerId: String, val novelId: String) {
        @Resource("/glossary")
        class Glossary(val parent: Id)

        @Resource("/chapter/{chapterId}")
        class Chapter(val parent: Id, val chapterId: String)

        @Resource("/translate-v2/{translatorId}")
        class TranslateV2(val parent: Id, val translatorId: TranslatorId) {

            @Resource("/chapter-task/{chapterId}")
            class ChapterTask(val parent: TranslateV2, val chapterId: String, val sync: Boolean)

            @Resource("/metadata")
            class Metadata(val parent: TranslateV2)

            @Resource("/chapter/{chapterId}")
            class Chapter(val parent: TranslateV2, val chapterId: String)
        }

        @Resource("/file")
        class File(
            val parent: Id,
            val mode: NovelFileMode,
            val translationsMode: NovelFileTranslationsMode,
            val translations: kotlin.collections.List<TranslatorId> = emptyList(),
            val type: NovelFileType,
            val filename: String,
        )
    }
}

fun Route.routeWebNovel() {
    val service by inject<WebNovelApi>()
    val translateV2Service by inject<WebNovelTranslateV2Api>()

    authenticateDb(optional = true) {
        get<WebNovelRes.List> { loc ->
            val user = call.userOrNull()
            call.tryRespond {
                service.list(
                    user = user,
                    queryString = loc.query?.ifBlank { null },
                    filterProvider = loc.provider,
                    filterType = when (loc.type) {
                        1 -> WebNovelFilter.Type.连载中
                        2 -> WebNovelFilter.Type.已完结
                        3 -> WebNovelFilter.Type.短篇
                        else -> WebNovelFilter.Type.全部
                    },
                    filterLevel = when (loc.level) {
                        1 -> WebNovelFilter.Level.一般向
                        2 -> WebNovelFilter.Level.R18
                        else -> WebNovelFilter.Level.全部
                    },
                    filterTranslate = when (loc.translate) {
                        1 -> WebNovelFilter.Translate.GPT3
                        2 -> WebNovelFilter.Translate.Sakura
                        else -> WebNovelFilter.Translate.全部
                    },
                    filterSort = when (loc.sort) {
                        1 -> WebNovelFilter.Sort.点击
                        2 -> WebNovelFilter.Sort.相关
                        else -> WebNovelFilter.Sort.更新
                    },
                    page = loc.page,
                    pageSize = loc.pageSize,
                )
            }
        }
    }
    get<WebNovelRes.Rank> { loc ->
        val options = call.request.queryParameters.toMap().mapValues { it.value.first() }
        call.tryRespond {
            val rank = service.listRank(providerId = loc.providerId, options = options)
            call.caching = CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 3600 * 2))
            rank
        }
    }

    // Get
    authenticateDb(optional = true) {
        get<WebNovelRes.Id> { loc ->
            val user = call.userOrNull()
            call.tryRespond {
                service.getMetadata(
                    user = user,
                    providerId = loc.providerId,
                    novelId = loc.novelId,
                )
            }
        }
    }
    get<WebNovelRes.Id.Chapter> { loc ->
        call.tryRespond {
            service.getChapter(
                providerId = loc.parent.providerId,
                novelId = loc.parent.novelId,
                chapterId = loc.chapterId,
            )
        }
    }

    authenticateDb {
        // Update
        post<WebNovelRes.Id> { loc ->
            @Serializable
            class Body(
                val title: String,
                val introduction: String,
                val wenkuId: String,
                val toc: Map<String, String>,
            )

            val user = call.user()
            val body = call.receive<Body>()
            call.tryRespond {
                service.updateMetadata(
                    user = user,
                    providerId = loc.providerId,
                    novelId = loc.novelId,
                    title = body.title,
                    introduction = body.introduction,
                    wenkuId = body.wenkuId,
                    toc = body.toc,
                )
            }
        }
        put<WebNovelRes.Id.Glossary> { loc ->
            val user = call.user()
            val body = call.receive<Map<String, String>>()
            call.tryRespond {
                service.updateGlossary(
                    user = user,
                    providerId = loc.parent.providerId,
                    novelId = loc.parent.novelId,
                    glossary = body,
                )
            }
        }

        // TranslateV2
        get<WebNovelRes.Id.TranslateV2> { loc ->
            call.tryRespond {
                translateV2Service.getTranslateTask(
                    providerId = loc.parent.providerId,
                    novelId = loc.parent.novelId,
                    translatorId = loc.translatorId,
                )
            }
        }
        post<WebNovelRes.Id.TranslateV2.ChapterTask> { loc ->
            call.tryRespond {
                translateV2Service.getChapterTranslateTask(
                    providerId = loc.parent.parent.providerId,
                    novelId = loc.parent.parent.novelId,
                    translatorId = loc.parent.translatorId,
                    chapterId = loc.chapterId,
                    sync = loc.sync,
                )
            }
        }
        post<WebNovelRes.Id.TranslateV2.Metadata> { loc ->
            @Serializable
            class Body(
                val title: String? = null,
                val introduction: String? = null,
                val toc: Map<String, String>,
            )

            val body = call.receive<Body>()
            call.tryRespond {
                translateV2Service.updateMetadataTranslation(
                    providerId = loc.parent.parent.providerId,
                    novelId = loc.parent.parent.novelId,
                    title = body.title,
                    introduction = body.introduction,
                    toc = body.toc,
                )
            }
        }
        post<WebNovelRes.Id.TranslateV2.Chapter> { loc ->
            @Serializable
            class Body(
                val glossaryId: String? = null,
                val paragraphsVi: List<String>,
                val sakuraVersion: String? = null,
            )

            val body = call.receive<Body>()
            call.tryRespond {
                translateV2Service.updateChapterTranslation(
                    providerId = loc.parent.parent.providerId,
                    novelId = loc.parent.parent.novelId,
                    translatorId = loc.parent.translatorId,
                    chapterId = loc.chapterId,
                    glossaryId = body.glossaryId,
                    paragraphsVi = body.paragraphsVi,
                    sakuraVersion = body.sakuraVersion,
                )
            }
        }
    }

    // File
    get<WebNovelRes.Id.File> { loc ->
        call.tryRespondRedirect {
            val path = service.updateFile(
                providerId = loc.parent.providerId,
                novelId = loc.parent.novelId,
                mode = loc.mode,
                translationsMode = loc.translationsMode,
                translations = loc.translations,
                type = loc.type,
            )
            val encodedFilename = loc.filename.encodeURLParameter(spaceToPlus = true)
            "/files-temp/web/${path.encodeURLParameter()}?filename=${encodedFilename}"
        }
    }
}

private fun throwNovelNotFound(): Nothing =
    throwNotFound("小说不存在")

private val disgustingFascistNovelList = mapOf(
    Syosetu.id to listOf(
        "n0646ie",
        "n8926ic",
        "n4583he",
        "n6465co",
        "n4357cw",
        "n9603hk",
        "n5149kv",
        "n3756im",
    ),
    Kakuyomu.id to listOf(
        "16816927860373250234",
        "16817330660019717771",
        "1177354054901629921",
        "16818093082836701336",
        "16817330661737648260",
        "16818622170290655590",
        "16818093088081078289",
    ),
    Hameln.id to listOf(
        "291561",
        "1472",
        "363542",
        "67369",
    ),
    Pixiv.id to listOf(
        "12802876",
    ),
)

private fun validateId(providerId: String, novelId: String) {
    if (providerId == Syosetu.id && novelId != novelId.lowercase()) {
        throw BadRequestException("成为小说家id应当小写")
    }
    disgustingFascistNovelList.get(providerId)?.let {
        if (novelId in it) {
            throw BadRequestException("该小说包含法西斯内容，不予显示")
        }
    }
}

class WebNovelApi(
    private val metadataRepo: WebNovelMetadataRepository,
    private val chapterRepo: WebNovelChapterRepository,
    private val fileRepo: WebNovelFileRepository,
    private val userFavoredRepo: UserFavoredRepository,
    private val favoredRepo: WebNovelFavoredRepository,
    private val historyRepo: WebNovelReadHistoryRepository,
    private val wenkuMetadataRepo: WenkuNovelMetadataRepository,
    private val operationHistoryRepo: OperationHistoryRepository,
) {
    suspend fun list(
        user: User?,
        queryString: String?,
        filterProvider: String,
        filterType: WebNovelFilter.Type,
        filterLevel: WebNovelFilter.Level,
        filterTranslate: WebNovelFilter.Translate,
        filterSort: WebNovelFilter.Sort,
        page: Int,
        pageSize: Int,
    ): Page<WebNovelOutlineDto> {
        validatePageNumber(page)
        validatePageSize(pageSize)

        val filterProviderParsed = if (filterProvider.isEmpty()) {
            return emptyPage()
        } else {
            filterProvider.split(",")
        }

        val filterLevelAllowed = if (user != null && user.isOldAss()) {
            filterLevel
        } else {
            WebNovelFilter.Level.一般向
        }

        return metadataRepo
            .search(
                userId = user?.id,
                userQuery = queryString,
                filterProvider = filterProviderParsed,
                filterType = filterType,
                filterLevel = filterLevelAllowed,
                filterTranslate = filterTranslate,
                filterSort = filterSort,
                page = page,
                pageSize = pageSize,
            )
            .map { it.asDto() }
    }

    suspend fun listRank(
        providerId: String,
        options: Map<String, String>,
    ): Page<WebNovelOutlineDto> {
        return metadataRepo
            .listRank(providerId, options)
            .getOrElse { throwInternalServerError("从源站获取失败:" + it.message) }
            .map { it.asDto() }
    }

    // Get
    @Serializable
    data class NovelTocItemDto(
        val titleJp: String,
        val titleVi: String?,
        val chapterId: String?,
        val createAt: Long?,
    )

    private fun WebNovelTocItem.asDto() =
        NovelTocItemDto(
            titleJp = titleJp,
            titleVi = titleVi,
            chapterId = chapterId,
            createAt = createAt?.epochSeconds,
        )

    @Serializable
    data class NovelDto(
        val wenkuId: String?,
        val titleJp: String,
        val titleVi: String?,
        val authors: List<WebNovelAuthor>,
        val type: WebNovelType?,
        val attentions: List<WebNovelAttention>,
        val keywords: List<String>,
        val points: Int?,
        val totalCharacters: Int?,
        val introductionJp: String,
        val introductionVi: String?,
        val glossary: Map<String, String>,
        val toc: List<NovelTocItemDto>,
        val visited: Long,
        val syncAt: Long,
        val favored: String?,
        val lastReadChapterId: String?,
        val jp: Long,
        val baidu: Long,
        val youdao: Long,
        val gpt: Long,
        val sakura: Long,
    )

    private suspend fun buildNovelDto(
        novel: WebNovel,
        user: User?,
    ): NovelDto {
        val dto = NovelDto(
            wenkuId = novel.wenkuId,
            titleJp = novel.titleJp,
            titleVi = novel.titleVi,
            authors = novel.authors,
            type = novel.type,
            attentions = novel.attentions,
            keywords = novel.keywords,
            points = novel.points,
            totalCharacters = novel.totalCharacters,
            introductionJp = novel.introductionJp,
            introductionVi = novel.introductionVi,
            glossary = novel.glossary,
            toc = novel.toc.map { it.asDto() },
            visited = novel.visited,
            syncAt = novel.syncAt.epochSeconds,
            favored = null,
            lastReadChapterId = null,
            jp = novel.jp,
            baidu = novel.baidu,
            youdao = novel.youdao,
            gpt = novel.gpt,
            sakura = novel.sakura,
        )
        return if (user == null) {
            dto
        } else {
            val novelId = novel.id.toHexString()
            val favored = favoredRepo
                .getFavoredId(user.id, novelId)
            val history = historyRepo.getReaderHistory(user.id, novelId)
            dto.copy(
                favored = favored,
                lastReadChapterId = history?.chapterId,
            )
        }
    }

    suspend fun getMetadata(
        user: User?,
        providerId: String,
        novelId: String,
    ): NovelDto {
        validateId(providerId, novelId)
        val novel = metadataRepo.getNovelAndSave(providerId, novelId)
            .getOrElse {
                if (it is NovelIdShouldBeReplacedException) {
                    throwBadRequest(it.message!!)
                } else {
                    throwInternalServerError("从源站获取失败:" + it.message)
                }
            }
        val dto = buildNovelDto(novel, user)
        if (user != null) {
            metadataRepo.increaseVisited(
                userIdOrIp = user.id,
                providerId = novel.providerId,
                novelId = novel.novelId,
            )
        }
        return dto
    }

    @Serializable
    data class ChapterDto(
        val titleJp: String,
        val titleVi: String?,
        val prevId: String?,
        val nextId: String?,
        val paragraphs: List<String>,
        val baiduParagraphs: List<String>?,
        val youdaoParagraphs: List<String>?,
        val gptParagraphs: List<String>?,
        val sakuraParagraphs: List<String>?,
    )

    suspend fun getChapter(
        providerId: String,
        novelId: String,
        chapterId: String,
    ): ChapterDto {
        validateId(providerId, novelId)
        val novel = metadataRepo.getNovelAndSave(providerId, novelId)
            .getOrElse { throwInternalServerError("从源站获取失败:" + it.message) }

        val toc = novel.toc.filter { it.chapterId != null }
        val currIndex = toc.indexOfFirst { it.chapterId == chapterId }
        if (currIndex == -1) throwInternalServerError("章节不在目录中")

        val chapter = chapterRepo.getOrSyncRemote(providerId, novelId, chapterId)
            .getOrElse { throwInternalServerError("从源站获取失败:" + it.message) }

        return ChapterDto(
            titleJp = toc[currIndex].titleJp,
            titleVi = toc[currIndex].titleVi,
            prevId = toc.getOrNull(currIndex - 1)?.chapterId,
            nextId = toc.getOrNull(currIndex + 1)?.chapterId,
            paragraphs = chapter.paragraphs,
            baiduParagraphs = chapter.baiduParagraphs,
            youdaoParagraphs = chapter.youdaoParagraphs,
            gptParagraphs = chapter.gptParagraphs,
            sakuraParagraphs = chapter.sakuraParagraphs,
        )
    }

    // Update
    suspend fun updateMetadata(
        user: User,
        providerId: String,
        novelId: String,
        title: String,
        introduction: String,
        wenkuId: String,
        toc: Map<String, String>,
    ) {
        user.shouldBeOldAss()

        if (wenkuId.isNotBlank() && wenkuMetadataRepo.get(wenkuId) == null) {
            throwNotFound("文库版不存在")
        }

        val metadata = metadataRepo.get(providerId, novelId)
            ?: throwNovelNotFound()

        val tocZh = mutableMapOf<Int, String>()
        val tocRecord = mutableListOf<Operation.WebEdit.Toc>()
        metadata.toc.forEachIndexed { index, item ->
            val newTitleZh = toc[item.titleJp]
            if (newTitleZh != null && newTitleZh != item.titleVi) {
                tocZh[index] = newTitleZh
                tocRecord.add(
                    Operation.WebEdit.Toc(
                        jp = item.titleJp,
                        old = item.titleVi,
                        new = newTitleZh,
                    )
                )
            }
        }

        val originWenkuId = metadata.wenkuId
        val targetWenkuId = wenkuId.takeIf { it.isNotBlank() }
        if (originWenkuId != targetWenkuId) {
            metadataRepo.updateWenkuId(
                providerId = providerId,
                novelId = novelId,
                wenkuId = wenkuId.takeIf { it.isNotBlank() },
            )
            val webId = "${providerId}/${novelId}"
            if (originWenkuId != null) {
                wenkuMetadataRepo.removeWebId(originWenkuId, webId)
            }
            if (targetWenkuId != null) {
                wenkuMetadataRepo.addWebId(targetWenkuId, webId)
            }
        }

        metadataRepo.updateTranslation(
            providerId = providerId,
            novelId = novelId,
            titleVi = title.takeIf { it.isNotBlank() },
            introductionVi = introduction.takeIf { it.isNotBlank() },
            tocZh = tocZh,
        )

        operationHistoryRepo.create(
            operator = ObjectId(user.id),
            operation = Operation.WebEdit(
                providerId = providerId,
                novelId = novelId,
                old = Operation.WebEdit.Data(
                    titleVi = metadata.titleVi,
                    introductionVi = metadata.introductionVi,
                ),
                new = Operation.WebEdit.Data(
                    titleVi = title,
                    introductionVi = introduction,
                ),
                toc = tocRecord,
            )
        )
    }

    suspend fun updateGlossary(
        user: User,
        providerId: String,
        novelId: String,
        glossary: Map<String, String>,
    ) {
        user.shouldBeOldAss()
        val novel = metadataRepo.get(providerId, novelId)
            ?: throwNovelNotFound()
        if (novel.glossary == glossary)
            throwBadRequest("修改为空")
        metadataRepo.updateGlossary(
            providerId = providerId,
            novelId = novelId,
            glossary = glossary,
        )
        operationHistoryRepo.create(
            operator = ObjectId(user.id),
            operation = Operation.WebEditGlossary(
                providerId = providerId,
                novelId = novelId,
                old = novel.glossary,
                new = glossary,
            )
        )
    }

    // File
    suspend fun updateFile(
        providerId: String,
        novelId: String,
        mode: NovelFileMode,
        translationsMode: NovelFileTranslationsMode,
        translations: List<TranslatorId>,
        type: NovelFileType,
    ): String {
        return fileRepo.makeFile(
            providerId = providerId,
            novelId = novelId,
            mode = mode,
            translationsMode = translationsMode,
            translations = translations.distinct(),
            type = type,
        ) ?: throwNovelNotFound()
    }
}

class WebNovelTranslateV2Api(
    private val metadataRepo: WebNovelMetadataRepository,
    private val chapterRepo: WebNovelChapterRepository,
) {
    @Serializable
    data class TranslateTaskDto(
        val titleJp: String,
        val titleVi: String?,
        val introductionJp: String,
        val introductionVi: String?,
        val glossaryUuid: String,
        val glossary: Map<String, String>,
        val toc: List<TocItem>,
    ) {
        @Serializable
        data class TocItem(
            val chapterId: String?,
            val titleJp: String,
            val titleVi: String?,
            val glossaryUuid: String?,
        )
    }

    suspend fun getTranslateTask(
        providerId: String,
        novelId: String,
        translatorId: TranslatorId,
    ): TranslateTaskDto {
        validateId(providerId, novelId)

        val novel = metadataRepo.getNovelAndSave(providerId, novelId, 10)
            .getOrElse { throwInternalServerError("从源站获取失败:" + it.message) }

        val chapterTranslationOutlines = chapterRepo.getTranslationOutlines(
            providerId = providerId,
            novelId = novelId,
            translatorId = translatorId,
        )
        val toc = novel.toc.map { item ->
            if (item.chapterId == null) {
                return@map TranslateTaskDto.TocItem(
                    chapterId = null,
                    titleJp = item.titleJp,
                    titleVi = item.titleVi,
                    glossaryUuid = null,
                )
            }

            val chapterTranslationOutline = chapterTranslationOutlines.find {
                it.chapterId == item.chapterId
            }
            val glossaryUuid = if (chapterTranslationOutline?.translated != true) {
                null
            } else if (
                translatorId == TranslatorId.Sakura && chapterTranslationOutline.sakuraVersion != "0.9"
            ) {
                "sakura outdated"
            } else {
                chapterTranslationOutline.glossaryUuid ?: "no glossary"
            }
            TranslateTaskDto.TocItem(
                chapterId = item.chapterId,
                titleJp = item.titleJp,
                titleVi = item.titleVi,
                glossaryUuid = glossaryUuid
            )
        }
        return TranslateTaskDto(
            titleJp = novel.titleJp,
            titleVi = novel.titleVi,
            introductionJp = novel.introductionJp,
            introductionVi = novel.introductionVi,
            glossaryUuid = novel.glossaryUuid ?: "no glossary",
            glossary = novel.glossary,
            toc = toc,
        )
    }

    @Serializable
    data class ChapterTranslateTaskDto(
        val paragraphJp: List<String>,
        val oldParagraphVi: List<String>?,
        val glossaryId: String,
        val glossary: Map<String, String>,
        val oldGlossaryId: String?,
        val oldGlossary: Map<String, String>,
    )

    suspend fun getChapterTranslateTask(
        providerId: String,
        novelId: String,
        translatorId: TranslatorId,
        chapterId: String,
        sync: Boolean,
    ): ChapterTranslateTaskDto {
        val novel = metadataRepo.get(providerId, novelId)
            ?: throwNovelNotFound()

        val chapter = chapterRepo.getOrSyncRemote(
            providerId = providerId,
            novelId = novelId,
            chapterId = chapterId,
            forceSync = sync,
        ).getOrElse {
            throwInternalServerError("从源站获取失败:" + it.message)
        }

        val (oldGlossaryIdRaw, oldGlossary, oldTranslation) = chapter.run {
            when (translatorId) {
                TranslatorId.Baidu -> Triple(baiduGlossaryUuid, baiduGlossary, baiduParagraphs)
                TranslatorId.Youdao -> Triple(youdaoGlossaryUuid, youdaoGlossary, youdaoParagraphs)
                TranslatorId.Gpt -> Triple(gptGlossaryUuid, gptGlossary, gptParagraphs)
                TranslatorId.Sakura -> Triple(sakuraGlossaryUuid, sakuraGlossary, sakuraParagraphs)
            }
        }

        val sakuraOutdated =
            (translatorId == TranslatorId.Sakura && chapter.sakuraVersion != "0.9")
        val oldGlossaryId = if (oldTranslation == null) {
            null
        } else if (sakuraOutdated) {
            "sakura outdated"
        } else {
            oldGlossaryIdRaw ?: "no glossary"
        }

        return ChapterTranslateTaskDto(
            paragraphJp = chapter.paragraphs,
            oldParagraphVi = oldTranslation.takeIf { !sakuraOutdated },
            glossaryId = novel.glossaryUuid ?: "no glossary",
            glossary = novel.glossary,
            oldGlossaryId = oldGlossaryId,
            oldGlossary = oldGlossary ?: emptyMap(),
        )
    }

    suspend fun updateMetadataTranslation(
        providerId: String,
        novelId: String,
        title: String?,
        introduction: String?,
        toc: Map<String, String>,
    ) {
        val metadata = metadataRepo.get(providerId, novelId)
            ?: throwNovelNotFound()

        val tocZh = mutableMapOf<Int, String>()
        metadata.toc.forEachIndexed { index, item ->
            val newTitleZh = toc[item.titleJp]
            if (newTitleZh != null) {
                tocZh[index] = newTitleZh
            }
        }

        if (title == null &&
            introduction == null &&
            tocZh.isEmpty()
        ) return

        metadataRepo.updateTranslation(
            providerId = providerId,
            novelId = novelId,
            titleVi = title ?: metadata.titleVi,
            introductionVi = introduction ?: metadata.introductionVi,
            tocZh = tocZh,
        )
    }

    @Serializable
    data class TranslateStateDto(
        val jp: Long,
        val vi: Long,
    )

    suspend fun updateChapterTranslation(
        providerId: String,
        novelId: String,
        chapterId: String,
        translatorId: TranslatorId,
        glossaryId: String?,
        paragraphsVi: List<String>,
        sakuraVersion: String?,
    ): TranslateStateDto {
        if (translatorId == TranslatorId.Sakura && sakuraVersion != "0.9") {
            throwBadRequest("旧版本Sakura不再允许上传")
        }

        val novel = metadataRepo.get(providerId, novelId)
            ?: throwNovelNotFound()
        if ((glossaryId ?: "no glossary") != (novel.glossaryUuid ?: "no glossary")) {
            throwBadRequest("术语表失效")
        }

        val chapter = chapterRepo.get(providerId, novelId, chapterId)
            ?: throwNotFound("章节不存在")
        if (chapter.paragraphs.size != paragraphsVi.size) {
            throwBadRequest("翻译文本长度不匹配")
        }

        val vi = chapterRepo.updateTranslation(
            providerId = providerId,
            novelId = novelId,
            chapterId = chapterId,
            translatorId = translatorId,
            glossary = novel.glossaryUuid?.let { Glossary(it, novel.glossary) },
            paragraphsVi = paragraphsVi,
        )
        return TranslateStateDto(jp = novel.jp, vi = vi)
    }
}

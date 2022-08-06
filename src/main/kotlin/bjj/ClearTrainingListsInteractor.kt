package bjj

import GI_TRAINING_LIST_PAGE_KEY
import NO_GI_TRAINING_LIST_PAGE_KEY
import dotEnv
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import notionClient
import org.jraf.klibnotion.model.base.UuidString
import org.jraf.klibnotion.model.block.Block
import org.jraf.klibnotion.model.block.MutableBlockList
import org.jraf.klibnotion.model.block.ToDoBlock
import org.jraf.klibnotion.model.richtext.RichTextList
import requireVariable

class ClearTrainingListsInteractor {

    companion object {
        private val giPageId = PageId(dotEnv.requireVariable(GI_TRAINING_LIST_PAGE_KEY))
        private val noGiPageId = PageId(dotEnv.requireVariable(NO_GI_TRAINING_LIST_PAGE_KEY))
    }

    @JvmInline
    value class PageId(val value: String)

    private val pageIdToDescription = mapOf(
        giPageId to "Gi Page",
        noGiPageId to "NoGi Page",
    )

    context(PipelineContext<Unit, ApplicationCall>) suspend fun clearGi() {
        clearBjj(giPageId)
    }

    context(PipelineContext<Unit, ApplicationCall>) suspend fun clearNoGi() {
        clearBjj(noGiPageId)
    }

    context(PipelineContext<Unit, ApplicationCall>) private suspend fun clearBjj(pageId: PageId) {
        fun List<Block>.getTodosToUncheck(
            result: MutableMap<UuidString, MutableBlockList> = mutableMapOf()
        ): Map<String, MutableBlockList> {
            for (block in this) {
                val children by lazy { block.children }
                if (block is ToDoBlock && block.checked) {
                    result[block.id] = MutableBlockList().toDo(
                        richTextList = block.text ?: RichTextList(),
                        checked = false
                    )
                } else if (children != null) {
                    children!!.getTodosToUncheck(result)
                }
            }

            return result
        }

        notionClient
            .blocks
            .getAllBlockListRecursively(pageId.value)
            .getTodosToUncheck()
            .forEach { (id, blocks) ->
                notionClient.blocks.updateBlock(id = id, blocks.first())
            }
        call.respondText("Done with ${pageIdToDescription.getValue(pageId)}")
    }
}
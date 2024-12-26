import com.github.jengelman.gradle.plugins.shadow.relocation.CacheableRelocator
import com.github.jengelman.gradle.plugins.shadow.relocation.RelocateClassContext
import com.github.jengelman.gradle.plugins.shadow.relocation.RelocatePathContext
import com.github.jengelman.gradle.plugins.shadow.relocation.Relocator

@CacheableRelocator
class KeywordRelocator : Relocator {
    override fun canRelocatePath(path: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun relocatePath(context: RelocatePathContext): String {
        TODO("Not yet implemented")
    }

    override fun canRelocateClass(className: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun relocateClass(context: RelocateClassContext): String {
        TODO("Not yet implemented")
    }

    override fun applyToSourceContent(sourceContent: String): String {
        TODO("Not yet implemented")
    }
}
package kdockfx

abstract class DetachableTabPaneFactory {

    fun create(source: DetachableTabPane) : DetachableTabPane {
        val detachableTabPane = DetachableTabPane()
        with(detachableTabPane) {
            scope = source.scope
            tabClosingPolicy = source.tabClosingPolicy
            closeIfEmpty = true
            detachableTabPaneFactory = source.detachableTabPaneFactory
            dropHint = source.dropHint
            init(this)
        }
        return detachableTabPane
    }


    protected abstract fun init(newTabPane: DetachableTabPane?)
}
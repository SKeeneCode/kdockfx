package kdockfx

import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.ListChangeListener
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.skin.TabPaneSkin
import javafx.scene.input.DataFormat
import javafx.scene.input.DragEvent
import javafx.scene.input.TransferMode
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.stage.WindowEvent
import java.util.*
import java.util.function.Consumer

class DetachableTabPane : TabPane() {

    companion object {
        var DRAG_SOURCE: DetachableTabPane? = null
        var DRAGGED_TAB: Tab? = null
        val DATA_FORMAT = DataFormat("dragAwareTab")
    }

    val scopeProperty: StringProperty = SimpleStringProperty("")
    var scope: String by scopeProperty

    private val lstTabPoint = ArrayList<Double>()
    private var pos: Pos? = null
    private var dropIndex: Int = 0
    internal var closeIfEmpty: Boolean = false
    internal var detachableTabPaneFactory = object : DetachableTabPaneFactory() {

        override fun init(newTabPane: DetachableTabPane?) {
            //
        }

    }

    private val dockPosIndicator = StackPane()
    private val buttonTop = StackPane()
    private val buttonRight = StackPane()
    private val buttonBottom = StackPane()
    private val buttonLeft = StackPane()

    var siblingProvider: Consumer<DetachableTabPane>? = null
    var parentSplitPane: TabSplitPane? = null
    var dropHint = TabDropHint()

    init {
        styleClass.add("detachable-tab-pane")
        attachListeners()
        initDropButton()
    }

    private fun attachListeners() {

        sceneProperty().addListener { _, oldScene, newScene ->
            if (oldScene == null && newScene != null) {
                if (scene.window != null) {
                    Platform.runLater { initiateDragGesture(true) }
                } else {
                    scene.windowProperty().addListener { _, oldWindow, newWindow ->
                        if (oldWindow == null && newWindow != null) {
                            newWindow.addEventHandler(WindowEvent.WINDOW_SHOWN) {
                                initiateDragGesture(true)
                            }
                        }
                    }
                }
            }
        }

        setOnDragOver { event ->
            val source = DRAG_SOURCE ?: return@setOnDragOver
            if (scope == source.scope) {
                event.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
                repaintPath(event)
            }
            event.consume()
        }

        setOnDragExited { event ->
            val tabPaneSkin = skin
            if (tabPaneSkin is TabPaneSkin) {
                tabPaneSkin.children.remove(dropHint.path)
                tabPaneSkin.children.remove(dockPosIndicator)
                requestLayout()
            }
        }

        setOnDragEntered { event ->
            println(DRAG_SOURCE)
            val source = DRAG_SOURCE ?: return@setOnDragEntered
            if (scope != source.scope) return@setOnDragEntered
            calculateTabPoints()
            dockPosIndicator.layoutX = width / 2
            dockPosIndicator.layoutY = height / 2
            val tabPaneSkin = skin
            if (tabPaneSkin is TabPaneSkin) {
                if (!tabPaneSkin.children.contains(dropHint.path)) {
                    if (!tabs.isEmpty()) {
                        tabPaneSkin.children.add(dockPosIndicator)
                    }
                    repaintPath(event)
                    tabPaneSkin.children.add(dropHint.path)
                }
            }
        }

        setOnDragDropped { event ->
            val position = pos
            val draggedTab = DRAGGED_TAB ?: return@setOnDragDropped
            if (position != null) {
                placeTab(draggedTab, position)
                event.isDropCompleted = true
                event.consume()
                return@setOnDragDropped
            }

            if (DRAG_SOURCE != this) {
                this.tabs.add(dropIndex, draggedTab)
                event.isDropCompleted = true
            } else {
                val currentSelectionIndex = tabs.indexOf(draggedTab)
                if (dropIndex == currentSelectionIndex) return@setOnDragDropped
                tabs.add(dropIndex, draggedTab)
                event.isDropCompleted = true
            }

            if (event.isDropCompleted) {
                event.dragboard.setContent(null)
            }
            event.consume()
        }

        tabs.addListener(ListChangeListener { change ->
            while (change.next()) {
                if (change.wasAdded()) {
                    if (scene?.window?.isShowing == true) {
                        Platform.runLater {
                            clearGesture()
                            initiateDragGesture(true)
                            futureTabPoints()
                        }
                    }
                } else if (change.wasRemoved()) {
                    futureTabPoints()
                    if (DRAG_SOURCE == null) {
                        if (tabs.isEmpty()) {
                            removeTabPaneFromParent(this)
                        }
                    }
                }
            }
        })


    }

    fun placeTab(tab: Tab, pos: Pos) {
        val addToLast = (pos == Pos.CENTER_RIGHT || pos == Pos.BOTTOM_CENTER)
        val dt = detachableTabPaneFactory.create(this)
        dt.tabs.add(tab)

        var requestedOrientation = Orientation.HORIZONTAL
        if (pos == Pos.BOTTOM_CENTER || pos == Pos.TOP_CENTER) {
            requestedOrientation = Orientation.VERTICAL
        }

        var targetSplitPane = parentSplitPane

        var requestedIndex = 0
        if (targetSplitPane != null && requestedOrientation == targetSplitPane.orientation) {
            requestedIndex = targetSplitPane.items.indexOf(this)
        }

        if (pos == Pos.CENTER_RIGHT || pos == Pos.BOTTOM_CENTER) {
            requestedIndex++
        }

        // if there is no kdockfx.TabSplitPane parent create one
        if (targetSplitPane == null) {
            targetSplitPane = TabSplitPane().apply {
                orientation = requestedOrientation
            }

            val parent = parent as Pane
            val indexInParent = parent.children.indexOf(this)
            parent.children.remove(this)
            if (addToLast) {
                targetSplitPane.items.addAll(this, dt)
            } else {
                targetSplitPane.items.addAll(dt, this)
            }
            parent.children.add(indexInParent, targetSplitPane)
        } else {
            if (targetSplitPane.items.size == 1) {
                targetSplitPane.orientation = requestedOrientation
            }
            if (targetSplitPane.orientation != requestedOrientation) {
                val parent = targetSplitPane
                val indexInParent = parent.items.indexOf(this)
                parent.items.remove(this)

                targetSplitPane = TabSplitPane().apply {
                    orientation = requestedOrientation
                }

                if (addToLast) {
                    targetSplitPane.items.addAll(this, dt)
                } else {
                    targetSplitPane.items.addAll(dt, this)
                }

                parent.items.add(indexInParent, targetSplitPane)

            } else {
                targetSplitPane.items.add(requestedIndex, dt)
                val itemCount = targetSplitPane.items.size
                val dividerPos = DoubleArray(itemCount)
                dividerPos[0] = 1.0 / itemCount
                for (i in 1 until dividerPos.size) {
                    dividerPos[i] = dividerPos[i - 1] + dividerPos[0]
                }
                targetSplitPane.setDividerPositions(*dividerPos)
            }
        }

    }

    private fun initiateDragGesture(retryOnFailed: Boolean) {
        val tabHeader = getTabHeaderArea()
        if (tabHeader == null) {
            if (retryOnFailed) {
                val timer = Timer()
                timer.schedule(object : TimerTask() {
                    override fun run() {
                        initiateDragGesture(false)
                        timer.cancel()
                        timer.purge()
                    }
                }, 500)
            }
            return
        }
        val tabs = tabHeader.lookupAll(".tab")
        for (tab in tabs) {
            addGesture(this, tab)
        }
    }

    private fun futureTabPoints() {
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                calculateTabPoints()
                timer.cancel()
                timer.purge()
            }
        }, 1000)
    }

    private fun clearGesture() {
        val tabHeader = getTabHeaderArea() ?: return
        val tabs = tabHeader.lookupAll(".tab")
        for (tab in tabs) {
            tab.setOnDragDetected {  }
            tab.setOnDragDone {  }
        }
    }

    private fun addGesture(tabPane: TabPane, node: Node) {
        node.lookup(".tab-container").lookup(".tab-close-button").onMousePressed = null
        node.lookup(".tab-container").lookup(".tab-close-button").setOnMouseReleased {
            val tab = tabPane.selectionModel.selectedItem
            tabPane.tabs.remove(tab)
        }

        node.setOnDragDetected { event ->
            val tab = tabPane.selectionModel.selectedItem
            if (tab is DetachableTab && !tab.isDetachable) return@setOnDragDetected
            val dragBoard = node.startDragAndDrop(*TransferMode.ANY)
            dragBoard.setDragView(node.snapshot(null, null), -20.0, 0.0)
            val dragContent = hashMapOf<DataFormat, Any>()
            dragContent[DATA_FORMAT] = "test"
            DRAG_SOURCE = this
            DRAGGED_TAB = tab

            // TODO "stop undocking when dragging off"

            tabs.remove(DRAGGED_TAB)
            dragBoard.setContent(dragContent)
            event.consume()
        }

        node.setOnDragDone { event ->
            val draggedTab = DRAGGED_TAB ?: return@setOnDragDone
            val source = DRAG_SOURCE ?: return@setOnDragDone
            if (draggedTab.tabPane == null) {
                source.tabs.add(draggedTab)
                source.selectionModel.select(draggedTab)

                // TODO "reallow docking"
            }
            if (source.tabs.isEmpty()) {
                removeTabPaneFromParent(source)
            } else {
                // TODO "readd listeners"
            }

            DRAG_SOURCE = null
            DRAGGED_TAB = null
            event.consume()
        }

    }

    private fun removeTabPaneFromParent(tabPaneToRemove: DetachableTabPane) {
        val sp = findParentTabSplitPane(tabPaneToRemove) ?: return
        if (!tabPaneToRemove.closeIfEmpty) {
            val sibling = findSibling(sp, tabPaneToRemove) ?: return
            sibling.closeIfEmpty = false
            val provider = siblingProvider
            if (provider != null) {
                provider.accept(sibling)
                sibling.siblingProvider = provider
            }
        }
        sp.items.remove(tabPaneToRemove)
        // TODO "remove tabpane from observable list"
        simplifySplitPane(sp)
    }



    private fun findSibling(sp: TabSplitPane, tabPaneToRemove: DetachableTabPane) : DetachableTabPane? {
        for (sibling in sp.items) {
            if (tabPaneToRemove != sibling
                && sibling is DetachableTabPane
                && tabPaneToRemove.scope == sibling.scope) {
                return sibling
            }
        }
        for (sibling in sp.items) {
            if (sibling is TabSplitPane) {
                return findSibling(sibling, tabPaneToRemove)
            }
        }
        return null
    }

    private fun simplifySplitPane(sp: TabSplitPane) {
        if (sp.items.size != 1) return
        val content = sp.items[0]
        val parent = findParentTabSplitPane(sp)
        if (parent != null) {
            val index = parent.items.indexOf(sp)
            parent.items.remove(sp)
            parent.items.add(index, content)
            simplifySplitPane(parent)
        }
    }

    private fun findParentTabSplitPane(control: Node) : TabSplitPane? {
        if (control is DetachableTabPane) return control.parentSplitPane
        if (control.parent == null) return null
        val lstSplitPane = control.scene.root.lookupAll(".split-pane")
        var parentSplitPane: TabSplitPane? = null
        for (node in lstSplitPane) {
            if (node is TabSplitPane) {
                if (node.items.contains(control)) {
                    parentSplitPane = node
                    break
                }
            }
        }
        return parentSplitPane
    }

    private fun repaintPath(event: DragEvent) {
        val hasTab = !tabs.isEmpty()
        if (hasTab && buttonLeft.contains(buttonLeft.screenToLocal(event.screenX, event.screenY))) {
            dropHint.refresh(0.0, 0.0, width / 2, height)
            pos = Pos.CENTER_LEFT
        } else if (hasTab && buttonRight.contains(buttonRight.screenToLocal(event.screenX, event.screenY))) {
            val pathWidth = width / 2
            dropHint.refresh(pathWidth, 0.0, pathWidth, height)
            pos = Pos.CENTER_RIGHT
        } else if (hasTab && buttonTop.contains(buttonTop.screenToLocal(event.screenX, event.screenY))) {
            dropHint.refresh(0.0, 0.0, width, height / 2)
            pos = Pos.TOP_CENTER
        } else if (hasTab && buttonBottom.contains(buttonBottom.screenToLocal(event.screenX, event.screenY))) {
            val pathHeight = height / 2
            dropHint.refresh(0.0, pathHeight, width, height - pathHeight)
            pos = Pos.BOTTOM_CENTER
        } else {
            pos = null
            var tabpos = -1.0
            for (i in 1 until lstTabPoint.size) {
                if (event.x < lstTabPoint[i]) {
                    tabpos = lstTabPoint[i - 1]
                    dropIndex = i - 1
                    break
                }
            }
            if (tabpos == -1.0) {
                val index = lstTabPoint.size - 1
                dropIndex = tabs.size
                if (index > -1) {
                    tabpos = lstTabPoint[index]
                }
            }
            dropHint.refresh(tabpos, width, height)
        }
    }

    private fun initDropButton() {
        buttonTop.styleClass.addAll("adjacent-drop", "drop-top")
        buttonRight.styleClass.addAll("adjacent-drop", "drop-right")
        buttonBottom.styleClass.addAll("adjacent-drop", "drop-bottom")
        buttonLeft.styleClass.addAll("adjacent-drop", "drop-left")

        StackPane.setAlignment(buttonTop, Pos.TOP_CENTER)
        StackPane.setAlignment(buttonRight, Pos.CENTER_RIGHT)
        StackPane.setAlignment(buttonBottom, Pos.BOTTOM_CENTER)
        StackPane.setAlignment(buttonLeft, Pos.CENTER_LEFT)

        val wrapper = StackPane()
        wrapper.styleClass.setAll("dock-pos-indicator")
        wrapper.children.addAll(buttonBottom, buttonLeft, buttonTop, buttonRight)

        dockPosIndicator.children.add(wrapper)

    }

    private fun calculateTabPoints() {
        lstTabPoint.clear()
        lstTabPoint.add(0.0)
        val tabHeader = getTabHeaderArea() ?: return
        val tabs: Set<Node> = tabHeader.lookupAll(".tab")
        val inset = localToScene(0.0, 0.0)
        for (tab in tabs) {
            val point = tab.localToScene(0.0, 0.0)
            val bounds = tab.layoutBounds
            lstTabPoint.add(point.x + bounds.width - inset.x)
        }
    }

    private fun getTabHeaderArea() : Node? {
        for (node in childrenUnmodifiable) {
            if (node.styleClass.contains("tab-header-area")) {
                return node
            }
        }
        return null
    }

    override fun getUserAgentStylesheet(): String {
        return DetachableTabPane.javaClass.getResource("tiwulfx-dock.css").toExternalForm()
    }

}

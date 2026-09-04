package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

// ==========================================
// SUPER NOTE (CANVAS) MODELS
// ==========================================

enum class CanvasTool {
    PAN_SELECT,
    PEN,
    HIGHLIGHTER,
    ERASER
}

sealed class CanvasElement {
    abstract val id: String
    abstract var x: Float
    abstract var y: Float
    abstract var width: Float
    abstract var height: Float

    data class Text(
        override val id: String,
        override var x: Float,
        override var y: Float,
        override var width: Float = 220f,
        override var height: Float = 100f,
        var text: String = "Nouveau texte",
        var fontSize: Float = 16f,
        var textColorHex: String = "#1E293B",
        var backgroundColorHex: String = "#FFFFFF",
        var isBold: Boolean = false,
        var isItalic: Boolean = false
    ) : CanvasElement()

    data class Image(
        override val id: String,
        override var x: Float,
        override var y: Float,
        override var width: Float = 240f,
        override var height: Float = 200f,
        val uri: String
    ) : CanvasElement()

    data class Shape(
        override val id: String,
        override var x: Float,
        override var y: Float,
        override var width: Float = 180f,
        override var height: Float = 120f,
        var shapeType: String = "RECTANGLE", // RECTANGLE, CIRCLE, LINE, ARROW
        var colorHex: String = "#4F46E5",
        var strokeWidth: Float = 4f,
        var isFilled: Boolean = false
    ) : CanvasElement()

    data class Table(
        override val id: String,
        override var x: Float,
        override var y: Float,
        override var width: Float = 320f,
        override var height: Float = 160f,
        var rows: Int = 3,
        var cols: Int = 3,
        var cells: MutableList<MutableList<String>> = MutableList(3) { MutableList(3) { "" } }
    ) : CanvasElement()

    data class Sticker(
        override val id: String,
        override var x: Float,
        override var y: Float,
        override var width: Float = 90f,
        override var height: Float = 90f,
        val emoji: String,
        val label: String
    ) : CanvasElement()

    data class PdfRef(
        override val id: String,
        override var x: Float,
        override var y: Float,
        override var width: Float = 260f,
        override var height: Float = 130f,
        val pdfId: Long,
        val pdfTitle: String,
        var pageNumber: Int = 1,
        var noteSnippet: String = ""
    ) : CanvasElement()
}

data class StrokePoint(val x: Float, val y: Float)

data class DrawingStroke(
    val id: String = java.util.UUID.randomUUID().toString(),
    val points: List<StrokePoint>,
    val colorHex: String = "#1E293B",
    val strokeWidth: Float = 5f,
    val isHighlighter: Boolean = false
)

data class CanvasData(
    val elements: List<CanvasElement> = emptyList(),
    val strokes: List<DrawingStroke> = emptyList(),
    val panX: Float = 0f,
    val panY: Float = 0f,
    val zoomScale: Float = 1f
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("panX", panX.toDouble())
        root.put("panY", panY.toDouble())
        root.put("zoomScale", zoomScale.toDouble())

        val elemArray = JSONArray()
        for (elem in elements) {
            val obj = JSONObject()
            obj.put("id", elem.id)
            obj.put("x", elem.x.toDouble())
            obj.put("y", elem.y.toDouble())
            obj.put("width", elem.width.toDouble())
            obj.put("height", elem.height.toDouble())

            when (elem) {
                is CanvasElement.Text -> {
                    obj.put("type", "TEXT")
                    obj.put("text", elem.text)
                    obj.put("fontSize", elem.fontSize.toDouble())
                    obj.put("textColorHex", elem.textColorHex)
                    obj.put("backgroundColorHex", elem.backgroundColorHex)
                    obj.put("isBold", elem.isBold)
                    obj.put("isItalic", elem.isItalic)
                }
                is CanvasElement.Image -> {
                    obj.put("type", "IMAGE")
                    obj.put("uri", elem.uri)
                }
                is CanvasElement.Shape -> {
                    obj.put("type", "SHAPE")
                    obj.put("shapeType", elem.shapeType)
                    obj.put("colorHex", elem.colorHex)
                    obj.put("strokeWidth", elem.strokeWidth.toDouble())
                    obj.put("isFilled", elem.isFilled)
                }
                is CanvasElement.Table -> {
                    obj.put("type", "TABLE")
                    obj.put("rows", elem.rows)
                    obj.put("cols", elem.cols)
                    val tableArray = JSONArray()
                    for (row in elem.cells) {
                        val rowArray = JSONArray()
                        for (cell in row) {
                            rowArray.put(cell)
                        }
                        tableArray.put(rowArray)
                    }
                    obj.put("cells", tableArray)
                }
                is CanvasElement.Sticker -> {
                    obj.put("type", "STICKER")
                    obj.put("emoji", elem.emoji)
                    obj.put("label", elem.label)
                }
                is CanvasElement.PdfRef -> {
                    obj.put("type", "PDF_REF")
                    obj.put("pdfId", elem.pdfId)
                    obj.put("pdfTitle", elem.pdfTitle)
                    obj.put("pageNumber", elem.pageNumber)
                    obj.put("noteSnippet", elem.noteSnippet)
                }
            }
            elemArray.put(obj)
        }
        root.put("elements", elemArray)

        val strokeArray = JSONArray()
        for (stroke in strokes) {
            val sObj = JSONObject()
            sObj.put("id", stroke.id)
            sObj.put("colorHex", stroke.colorHex)
            sObj.put("strokeWidth", stroke.strokeWidth.toDouble())
            sObj.put("isHighlighter", stroke.isHighlighter)
            val pArray = JSONArray()
            for (p in stroke.points) {
                val pObj = JSONObject()
                pObj.put("x", p.x.toDouble())
                pObj.put("y", p.y.toDouble())
                pArray.put(pObj)
            }
            sObj.put("points", pArray)
            strokeArray.put(sObj)
        }
        root.put("strokes", strokeArray)

        return root.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): CanvasData {
            if (jsonStr.isBlank()) return CanvasData()
            return try {
                val root = JSONObject(jsonStr)
                val panX = root.optDouble("panX", 0.0).toFloat()
                val panY = root.optDouble("panY", 0.0).toFloat()
                val zoomScale = root.optDouble("zoomScale", 1.0).toFloat()

                val elements = mutableListOf<CanvasElement>()
                val elemArray = root.optJSONArray("elements") ?: JSONArray()
                for (i in 0 until elemArray.length()) {
                    val obj = elemArray.getJSONObject(i)
                    val id = obj.optString("id", java.util.UUID.randomUUID().toString())
                    val x = obj.optDouble("x", 100.0).toFloat()
                    val y = obj.optDouble("y", 100.0).toFloat()
                    val width = obj.optDouble("width", 200.0).toFloat()
                    val height = obj.optDouble("height", 100.0).toFloat()
                    val type = obj.optString("type", "TEXT")

                    when (type) {
                        "TEXT" -> {
                            elements.add(
                                CanvasElement.Text(
                                    id = id,
                                    x = x,
                                    y = y,
                                    width = width,
                                    height = height,
                                    text = obj.optString("text", ""),
                                    fontSize = obj.optDouble("fontSize", 16.0).toFloat(),
                                    textColorHex = obj.optString("textColorHex", "#1E293B"),
                                    backgroundColorHex = obj.optString("backgroundColorHex", "#FFFFFF"),
                                    isBold = obj.optBoolean("isBold", false),
                                    isItalic = obj.optBoolean("isItalic", false)
                                )
                            )
                        }
                        "IMAGE" -> {
                            elements.add(
                                CanvasElement.Image(
                                    id = id,
                                    x = x,
                                    y = y,
                                    width = width,
                                    height = height,
                                    uri = obj.optString("uri", "")
                                )
                            )
                        }
                        "SHAPE" -> {
                            elements.add(
                                CanvasElement.Shape(
                                    id = id,
                                    x = x,
                                    y = y,
                                    width = width,
                                    height = height,
                                    shapeType = obj.optString("shapeType", "RECTANGLE"),
                                    colorHex = obj.optString("colorHex", "#4F46E5"),
                                    strokeWidth = obj.optDouble("strokeWidth", 4.0).toFloat(),
                                    isFilled = obj.optBoolean("isFilled", false)
                                )
                            )
                        }
                        "TABLE" -> {
                            val rows = obj.optInt("rows", 3)
                            val cols = obj.optInt("cols", 3)
                            val tableCells = MutableList(rows) { MutableList(cols) { "" } }
                            val cellsArray = obj.optJSONArray("cells")
                            if (cellsArray != null) {
                                for (r in 0 until minOf(rows, cellsArray.length())) {
                                    val rowArr = cellsArray.optJSONArray(r)
                                    if (rowArr != null) {
                                        for (c in 0 until minOf(cols, rowArr.length())) {
                                            tableCells[r][c] = rowArr.optString(c, "")
                                        }
                                    }
                                }
                            }
                            elements.add(
                                CanvasElement.Table(
                                    id = id,
                                    x = x,
                                    y = y,
                                    width = width,
                                    height = height,
                                    rows = rows,
                                    cols = cols,
                                    cells = tableCells
                                )
                            )
                        }
                        "STICKER" -> {
                            elements.add(
                                CanvasElement.Sticker(
                                    id = id,
                                    x = x,
                                    y = y,
                                    width = width,
                                    height = height,
                                    emoji = obj.optString("emoji", "⭐"),
                                    label = obj.optString("label", "Important")
                                )
                            )
                        }
                        "PDF_REF" -> {
                            elements.add(
                                CanvasElement.PdfRef(
                                    id = id,
                                    x = x,
                                    y = y,
                                    width = width,
                                    height = height,
                                    pdfId = obj.optLong("pdfId", 0L),
                                    pdfTitle = obj.optString("pdfTitle", "Document PDF"),
                                    pageNumber = obj.optInt("pageNumber", 1),
                                    noteSnippet = obj.optString("noteSnippet", "")
                                )
                            )
                        }
                    }
                }

                val strokes = mutableListOf<DrawingStroke>()
                val strokeArray = root.optJSONArray("strokes") ?: JSONArray()
                for (i in 0 until strokeArray.length()) {
                    val sObj = strokeArray.getJSONObject(i)
                    val sId = sObj.optString("id", java.util.UUID.randomUUID().toString())
                    val colorHex = sObj.optString("colorHex", "#1E293B")
                    val strokeWidth = sObj.optDouble("strokeWidth", 5.0).toFloat()
                    val isHighlighter = sObj.optBoolean("isHighlighter", false)

                    val pArray = sObj.optJSONArray("points") ?: JSONArray()
                    val points = mutableListOf<StrokePoint>()
                    for (pIdx in 0 until pArray.length()) {
                        val pObj = pArray.getJSONObject(pIdx)
                        points.add(
                            StrokePoint(
                                x = pObj.optDouble("x", 0.0).toFloat(),
                                y = pObj.optDouble("y", 0.0).toFloat()
                            )
                        )
                    }
                    strokes.add(
                        DrawingStroke(
                            id = sId,
                            points = points,
                            colorHex = colorHex,
                            strokeWidth = strokeWidth,
                            isHighlighter = isHighlighter
                        )
                    )
                }

                CanvasData(
                    elements = elements,
                    strokes = strokes,
                    panX = panX,
                    panY = panY,
                    zoomScale = zoomScale
                )
            } catch (e: Exception) {
                e.printStackTrace()
                CanvasData()
            }
        }
    }
}

// ==========================================
// MIND MAP MODELS
// ==========================================

data class MindMapNode(
    val id: String = java.util.UUID.randomUUID().toString(),
    var text: String,
    var x: Float,
    var y: Float,
    var parentId: String? = null,
    var colorHex: String = "#4F46E5",
    var shape: String = "ROUNDED" // ROUNDED, PILL, RECT
)

data class MindMapData(
    val nodes: List<MindMapNode> = emptyList(),
    val style: String = "MODERN_INDIGO", // MODERN_INDIGO, PASTEL_STUDY, DARK_NEON, MINIMALIST
    val panX: Float = 0f,
    val panY: Float = 0f,
    val zoomScale: Float = 1f
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("style", style)
        root.put("panX", panX.toDouble())
        root.put("panY", panY.toDouble())
        root.put("zoomScale", zoomScale.toDouble())
        val array = JSONArray()
        for (node in nodes) {
            val obj = JSONObject()
            obj.put("id", node.id)
            obj.put("text", node.text)
            obj.put("x", node.x.toDouble())
            obj.put("y", node.y.toDouble())
            if (node.parentId != null) {
                obj.put("parentId", node.parentId)
            }
            obj.put("colorHex", node.colorHex)
            obj.put("shape", node.shape)
            array.put(obj)
        }
        root.put("nodes", array)
        return root.toString()
    }

    companion object {
        fun defaultMap(title: String = "Mon Sujet"): MindMapData {
            val rootId = "root_node"
            val rootNode = MindMapNode(
                id = rootId,
                text = title.ifBlank { "Idée Principale" },
                x = 600f,
                y = 400f,
                parentId = null,
                colorHex = "#4F46E5",
                shape = "PILL"
            )
            val sub1 = MindMapNode(
                id = "node_1",
                text = "Définition",
                x = 350f,
                y = 250f,
                parentId = rootId,
                colorHex = "#06B6D4"
            )
            val sub2 = MindMapNode(
                id = "node_2",
                text = "Concepts clés",
                x = 850f,
                y = 250f,
                parentId = rootId,
                colorHex = "#10B981"
            )
            val sub3 = MindMapNode(
                id = "node_3",
                text = "Exemples & Citations",
                x = 600f,
                y = 600f,
                parentId = rootId,
                colorHex = "#F59E0B"
            )
            return MindMapData(nodes = listOf(rootNode, sub1, sub2, sub3))
        }

        fun fromJson(jsonStr: String): MindMapData {
            if (jsonStr.isBlank()) return defaultMap()
            return try {
                val root = JSONObject(jsonStr)
                val style = root.optString("style", "MODERN_INDIGO")
                val panX = root.optDouble("panX", 0.0).toFloat()
                val panY = root.optDouble("panY", 0.0).toFloat()
                val zoomScale = root.optDouble("zoomScale", 1.0).toFloat()

                val array = root.optJSONArray("nodes") ?: JSONArray()
                val list = mutableListOf<MindMapNode>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        MindMapNode(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            text = obj.optString("text", "Idée"),
                            x = obj.optDouble("x", 400.0).toFloat(),
                            y = obj.optDouble("y", 400.0).toFloat(),
                            parentId = if (obj.has("parentId") && !obj.isNull("parentId")) obj.getString("parentId") else null,
                            colorHex = obj.optString("colorHex", "#4F46E5"),
                            shape = obj.optString("shape", "ROUNDED")
                        )
                    )
                }
                if (list.isEmpty()) defaultMap() else MindMapData(nodes = list, style = style, panX = panX, panY = panY, zoomScale = zoomScale)
            } catch (e: Exception) {
                e.printStackTrace()
                defaultMap()
            }
        }
    }
}

package com.example.myrunapp.feature.run

import kotlin.math.hypot

/**
 * 轨迹标记的屏幕空间布局。
 *
 * 背景：GO/END 与里程标记以前是直接钉在经纬度上的圆点，绕圈/折返轨迹会让不同标记落到同一坐标，
 * 于是后添加的（或 zIndex 更高的）会把前面的完全盖住。这里把"要不要显示文字"这件事从地图层
 * 抽出来，用纯几何计算决定，Composable/地图层只负责按结果渲染。
 *
 * 设计约定：
 * - 起点/终点用旗标渲染（一个向上、一个向下展开），所以它们之间**永不互相遮挡**，也不参与互斥判断；
 * - 只有里程点会被降级（隐藏数字、退化成小圆点）；
 * - 当前定位点不参与布局：它每秒都在移动，纳入布局会导致每秒重排，且它本身只是"我在这"的指示点。
 */
internal enum class TrackLabelKind {
    START,
    END,
    KILOMETER
}

/** 屏幕空间候选标签，坐标为像素（地图投影后）。 */
internal data class TrackLabelCandidate(
    val index: Int,
    val kind: TrackLabelKind,
    val x: Float,
    val y: Float,
    val kilometer: Int? = null
)

/** 布局结果：每个候选都会得到一个结果，要么显示文字，要么降级成小圆点。 */
internal data class TrackLabelPlacement(
    val index: Int,
    val kind: TrackLabelKind,
    val text: String,
    val isDot: Boolean
)

/** 起终点旗标与里程圆点之间需要保留的间距（半径和，dp）。 */
private const val StartEndClearanceDp = 18f

/** 两个里程圆点之间需要保留的间距（半径和，dp）。 */
private const val KilometerClearanceDp = 11f

private const val StartEndMinGapDp = StartEndClearanceDp + KilometerClearanceDp
private const val KilometerMinGapDp = KilometerClearanceDp * 2f

/**
 * 计算每个标记最终形态。
 *
 * @param candidates 屏幕坐标下的候选标签（同一位置的多个里程点应传入多个候选）
 * @param density 屏幕密度，用于把 dp 阈值换成像素
 * @return 与 candidates 等长、下标一一对应的布局结果
 */
internal fun layoutTrackLabels(
    candidates: List<TrackLabelCandidate>,
    density: Float,
    startText: String = "GO",
    endText: String = "END"
): List<TrackLabelPlacement> {
    if (candidates.isEmpty()) return emptyList()
    val safeDensity = if (density > 0f) density else 1f
    // 用 index 作 key 而不是列表下标：调用方保证 index 唯一，布局层不假设它是 0..n-1
    val placements = HashMap<Int, TrackLabelPlacement>(candidates.size)

    // 起终点：旗标上下分开展开，永远显示文字，只作为"占位"参与里程点判定
    val anchorPoints = mutableListOf<FloatArray>()
    candidates.forEach { candidate ->
        if (candidate.kind == TrackLabelKind.KILOMETER) return@forEach
        val text = when (candidate.kind) {
            TrackLabelKind.START -> startText
            TrackLabelKind.END -> endText
            TrackLabelKind.KILOMETER -> ""
        }
        placements[candidate.index] = TrackLabelPlacement(
            index = candidate.index,
            kind = candidate.kind,
            text = text,
            isDot = false
        )
        anchorPoints += floatArrayOf(candidate.x, candidate.y)
    }

    val kilometers = candidates
        .filter { it.kind == TrackLabelKind.KILOMETER && it.kilometer != null }
        .sortedBy { it.kilometer }

    // 先按屏幕距离聚簇：同一簇（例如操场每 2.5 圈循环一次）只保留一个文字标签
    val clusters = mutableListOf<MutableList<TrackLabelCandidate>>()
    val kilometerGap = KilometerMinGapDp * safeDensity
    kilometers.forEach { kilometer ->
        val cluster = clusters.firstOrNull { group ->
            group.any { member -> distance(member, kilometer) < kilometerGap }
        }
        if (cluster != null) {
            cluster += kilometer
        } else {
            clusters += mutableListOf(kilometer)
        }
    }

    val startEndGap = StartEndMinGapDp * safeDensity
    clusters.forEach { cluster ->
        cluster.forEach { member ->
            placements[member.index] = TrackLabelPlacement(
                index = member.index,
                kind = member.kind,
                text = "",
                isDot = true
            )
        }
        // 簇内取编号最大的作为代表：跑动中"跑到第几公里"比"第 1 公里刻点在哪"更有价值
        val representative = cluster.maxByOrNull { it.kilometer ?: 0 } ?: return@forEach
        val blockedByStartEnd = anchorPoints.any { anchor ->
            hypot(anchor[0] - representative.x, anchor[1] - representative.y) < startEndGap
        }
        if (!blockedByStartEnd) {
            placements[representative.index] = TrackLabelPlacement(
                index = representative.index,
                kind = representative.kind,
                text = (representative.kilometer ?: 0).toString(),
                isDot = false
            )
        }
    }

    return candidates.map { candidate ->
        placements[candidate.index] ?: TrackLabelPlacement(
            index = candidate.index,
            kind = candidate.kind,
            text = "",
            isDot = true
        )
    }}

private fun distance(a: TrackLabelCandidate, b: TrackLabelCandidate): Float {
    return hypot(a.x - b.x, a.y - b.y)
}

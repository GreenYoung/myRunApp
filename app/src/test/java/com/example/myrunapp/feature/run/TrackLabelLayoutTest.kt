package com.example.myrunapp.feature.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackLabelLayoutTest {
    @Test
    fun emptyCandidates_produceEmptyResult() {
        val placements = layoutTrackLabels(emptyList(), density = 1f)

        assertTrue(placements.isEmpty())
    }

    @Test
    fun startAndEndAtSamePoint_areBothShown() {
        // 环线：起点与终点完全重合。旗标一个向上、一个向下，不需要互斥
        val placements = layoutTrackLabels(
            listOf(
                candidate(index = 0, kind = TrackLabelKind.START, x = 100f, y = 100f),
                candidate(index = 1, kind = TrackLabelKind.END, x = 100f, y = 100f)
            ),
            density = 1f
        )

        assertEquals(2, placements.size)
        assertFalse(placements[0].isDot)
        assertEquals("GO", placements[0].text)
        assertFalse(placements[1].isDot)
        assertEquals("END", placements[1].text)
    }

    @Test
    fun kilometersAtSamePoint_keepOnlyLargestText() {
        // 操场多圈：1/3/5 公里落在同一位置，只显示最新编号，其余降级成小圆点
        val placements = layoutTrackLabels(
            listOf(
                kilometerCandidate(index = 0, kilometer = 1, x = 50f, y = 50f),
                kilometerCandidate(index = 1, kilometer = 3, x = 51f, y = 50f),
                kilometerCandidate(index = 2, kilometer = 5, x = 50f, y = 51f)
            ),
            density = 1f
        )

        assertTrue(placements[0].isDot)
        assertTrue(placements[1].isDot)
        assertFalse(placements[2].isDot)
        assertEquals("5", placements[2].text)
    }

    @Test
    fun kilometersFarApart_areAllShown() {
        val placements = layoutTrackLabels(
            listOf(
                kilometerCandidate(index = 0, kilometer = 1, x = 0f, y = 0f),
                kilometerCandidate(index = 1, kilometer = 2, x = 0f, y = 100f)
            ),
            density = 1f
        )

        assertFalse(placements[0].isDot)
        assertEquals("1", placements[0].text)
        assertFalse(placements[1].isDot)
        assertEquals("2", placements[1].text)
    }

    @Test
    fun kilometersWithinGap_areClustered() {
        // 阈值 22dp：相距 21dp 视为同一位置
        val placements = layoutTrackLabels(
            listOf(
                kilometerCandidate(index = 0, kilometer = 1, x = 0f, y = 0f),
                kilometerCandidate(index = 1, kilometer = 2, x = 0f, y = 21f)
            ),
            density = 1f
        )

        assertTrue(placements[0].isDot)
        assertFalse(placements[1].isDot)
        assertEquals("2", placements[1].text)
    }

    @Test
    fun kilometerTooCloseToStart_isDotted() {
        // 起点旗标与里程点最小间距 29dp，10dp 处必须让位
        val placements = layoutTrackLabels(
            listOf(
                candidate(index = 0, kind = TrackLabelKind.START, x = 100f, y = 100f),
                kilometerCandidate(index = 1, kilometer = 1, x = 110f, y = 100f)
            ),
            density = 1f
        )

        assertFalse(placements[0].isDot)
        assertTrue(placements[1].isDot)
    }

    @Test
    fun kilometerFarFromStart_keepsText() {
        val placements = layoutTrackLabels(
            listOf(
                candidate(index = 0, kind = TrackLabelKind.START, x = 100f, y = 100f),
                kilometerCandidate(index = 1, kilometer = 1, x = 140f, y = 100f)
            ),
            density = 1f
        )

        assertFalse(placements[0].isDot)
        assertFalse(placements[1].isDot)
        assertEquals("1", placements[1].text)
    }

    @Test
    fun densityScalesTheGap() {
        // 相距 40px：1 倍密度下是 40dp（> 22dp，两个标签都显示）；2 倍密度下只有 20dp（< 22dp，聚合成一个）
        val candidates = listOf(
            kilometerCandidate(index = 0, kilometer = 1, x = 0f, y = 0f),
            kilometerCandidate(index = 1, kilometer = 2, x = 0f, y = 40f)
        )

        val oneToOne = layoutTrackLabels(candidates, density = 1f)
        val highDensity = layoutTrackLabels(candidates, density = 2f)

        assertFalse(oneToOne[0].isDot)
        assertFalse(oneToOne[1].isDot)
        assertTrue(highDensity[0].isDot)
        assertFalse(highDensity[1].isDot)
    }

    @Test
    fun placementsFollowCandidateIndexes() {
        val candidates = listOf(
            candidate(index = 2, kind = TrackLabelKind.START, x = 0f, y = 0f),
            candidate(index = 5, kind = TrackLabelKind.END, x = 300f, y = 300f),
            kilometerCandidate(index = 9, kilometer = 4, x = 150f, y = 150f)
        )

        val placements = layoutTrackLabels(candidates, density = 1f)

        assertEquals(listOf(2, 5, 9), placements.map { it.index })
        assertEquals(TrackLabelKind.START, placements[0].kind)
        assertEquals("4", placements[2].text)
    }

    private fun candidate(
        index: Int,
        kind: TrackLabelKind,
        x: Float,
        y: Float
    ): TrackLabelCandidate {
        return TrackLabelCandidate(index = index, kind = kind, x = x, y = y)
    }

    private fun kilometerCandidate(
        index: Int,
        kilometer: Int,
        x: Float,
        y: Float
    ): TrackLabelCandidate {
        return TrackLabelCandidate(
            index = index,
            kind = TrackLabelKind.KILOMETER,
            x = x,
            y = y,
            kilometer = kilometer
        )
    }
}

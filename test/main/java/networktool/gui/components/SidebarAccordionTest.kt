package main.java.networktool.gui.components

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.lang.String
import java.util.function.Consumer
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.Array
import kotlin.arrayOf
import kotlin.collections.ArrayList
import kotlin.collections.MutableList

internal class SidebarAccordionTest {
    private fun container(level: SidebarAccordion.AccessLevel?): JPanel {
        val sp = SidebarAccordion.build(ITEMS, level, Consumer { id: String? -> })
        return sp.getViewport().getView() as JPanel
    }

    /** Liest Header-Label und die Button-Menü-IDs jeder Gruppe in Reihenfolge aus.  */
    private fun groupHeaderLabels(container: JPanel): MutableList<Array<String>?> {
        val result: MutableList<Array<String>?> = ArrayList<Array<String>?>()
        val kids = container.getComponents()
        var i = 0
        while (i + 1 < kids.size) {
            if (kids[i] !is JPanel || kids[i + 1] !is JPanel) break
            val label = headerText(header)
            val ids: MutableList<String?> = ArrayList<String?>()
            for (c in content.getComponents()) if (c is JButton) ids.add(c.getClientProperty("menuId") as String?)
            result.add(arrayOf<String>(label, String.join(",", ids)))
            i += 2
        }
        return result
    }

    private fun headerText(header: JPanel): kotlin.String {
        for (c in header.getComponents()) if (c is JLabel) return c.getText().trim { it <= ' ' }
        return ""
    }

    // ── USER: admin-only Gruppe komplett unsichtbar ────────────────────────
    @Test
    fun user_adminOnlyGroupIsAbsent() {
        val groups = groupHeaderLabels(container(SidebarAccordion.AccessLevel.USER))
        Assertions.assertTrue(groups.stream().noneMatch { g: Array<kotlin.String?>? -> g!![0] == "GROUP_ADMIN" })
    }

    @Test
    fun user_onlyStandardGroupsRemain() {
        val groups = groupHeaderLabels(container(SidebarAccordion.AccessLevel.USER))
        Assertions.assertEquals(2, groups.size)
        Assertions.assertEquals("GROUP_A", groups.get(0)!![0])
        Assertions.assertEquals("GROUP_B", groups.get(1)!![0])
    }

    /** Regressionstest: Kind-Elemente einer gefilterten Sektion dürfen nicht in GROUP_A landen.  */
    @Test
    fun user_childOfHiddenGroup_doesNotLeakIntoPreviousGroup() {
        val groups = groupHeaderLabels(container(SidebarAccordion.AccessLevel.USER))
        val groupAItems = groups.get(0)!![1]
        Assertions.assertEquals("a1", groupAItems)
        Assertions.assertFalse(groupAItems.contains("x1"))
    }

    @Test
    fun user_adminOnlyItemUnderVisibleGroup_isFiltered() {
        val groups = groupHeaderLabels(container(SidebarAccordion.AccessLevel.USER))
        val groupBItems = groups.get(1)!![1]
        Assertions.assertEquals("b1", groupBItems)
        Assertions.assertFalse(groupBItems.contains("b2"))
    }

    // ── ADMIN: alles sichtbar ───────────────────────────────────────────────
    @Test
    fun admin_allGroupsPresent() {
        val groups = groupHeaderLabels(container(SidebarAccordion.AccessLevel.ADMIN))
        Assertions.assertEquals(3, groups.size)
        Assertions.assertEquals("GROUP_A", groups.get(0)!![0])
        Assertions.assertEquals("GROUP_ADMIN", groups.get(1)!![0])
        Assertions.assertEquals("GROUP_B", groups.get(2)!![0])
    }

    @Test
    fun admin_adminOnlyGroupContainsItsItem() {
        val groups = groupHeaderLabels(container(SidebarAccordion.AccessLevel.ADMIN))
        Assertions.assertEquals("x1", groups.get(1)!![1])
    }

    @Test
    fun admin_adminOnlyItemUnderVisibleGroup_isIncluded() {
        val groups = groupHeaderLabels(container(SidebarAccordion.AccessLevel.ADMIN))
        Assertions.assertEquals("b1,b2", groups.get(2)!![1])
    }

    // ── Struktur ─────────────────────────────────────────────────────────
    @Test
    fun build_firstStandardGroup_isInitiallyOpen() {
        val c = container(SidebarAccordion.AccessLevel.USER)
        val firstContent = c.getComponents()[1] as JPanel
        Assertions.assertTrue(firstContent.isVisible())
    }

    @Test
    fun build_secondGroup_isInitiallyClosed() {
        val c = container(SidebarAccordion.AccessLevel.USER)
        val secondContent = c.getComponents()[3] as JPanel
        Assertions.assertFalse(secondContent.isVisible())
    }

    companion object {
        @BeforeAll
        fun headless() {
            System.setProperty("java.awt.headless", "true")
        }

        private val ITEMS = arrayOf<Array<kotlin.String?>?>(
            arrayOf<kotlin.String?>(null, "GROUP_A", null, "false"),
            arrayOf<kotlin.String?>("a1", "ItemA1", null, "false"),
            arrayOf<kotlin.String?>(null, "GROUP_ADMIN", null, "true"),
            arrayOf<kotlin.String?>("x1", "ItemX1", null, "false"),
            arrayOf<kotlin.String?>(null, "GROUP_B", null, "false"),
            arrayOf<kotlin.String?>("b1", "ItemB1", null, "false"),
            arrayOf<kotlin.String?>("b2", "ItemB2", null, "true"),
        )
    }
}
package app.morphe.patches.youtube.misc.links

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.patches.youtube.misc.settings.PreferenceScreen
import app.morphe.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.util.matchAllMethodIndicesForEach
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/morphe/extension/youtube/patches/OpenLinksExternallyPatch;"

val openLinksExternallyPatch = bytecodePatch(
    name = "Open links externally",
    description = "Adds an option to always open links in your browser instead of with the in-app browser.",
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        PreferenceScreen.MISC.addPreferences(
            SwitchPreference("morphe_external_browser", summary = true),
        )

        Fingerprint(
            filters = listOf(
                string("android.support.customtabs.action.CustomTabsService")
            ),
            custom = { _, classDef ->
                !classDef.type.startsWith("Lapp/morphe/")
            }
        ).matchAllMethodIndicesForEach { index ->
            val register = getInstruction<OneRegisterInstruction>(index).registerA

            addInstructions(
                index + 1,
                """
                    invoke-static { v$register }, $EXTENSION_CLASS->getIntent(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$register
                """
            )
        }

        // Try a dedicated app (GitHub app for github.com links etc) before falling back to the browser.
        DescriptionAndPostUriFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static { p0, p1 }, $EXTENSION_CLASS->openInDedicatedApp(Landroid/content/Context;Landroid/net/Uri;)Landroid/net/Uri;
                move-result-object p1
                if-nez p1, :morphe_continue_browser
                const/16 p1, 0x1
                return p1
                :morphe_continue_browser
                nop
            """
        )
    }
}

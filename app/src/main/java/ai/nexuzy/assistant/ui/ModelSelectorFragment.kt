package ai.nexuzy.assistant.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import ai.nexuzy.assistant.llm.ModelManager

/**
 * ModelSelectorFragment — bottom sheet that shows all David AI model tiers.
 * Highlights compatible models. Recommends the best one for this device.
 * User can manually override and download/switch any model.
 */
class ModelSelectorFragment(
    private val modelManager: ModelManager,
    private val onModelSelected: (ModelManager.ModelInfo) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 60)
        }

        val recommended = modelManager.recommendedModel()
        val compatible  = modelManager.compatibleModels().map { it.modelId }.toSet()
        val ramLabel    = modelManager.getRamLabel()

        // Header
        TextView(requireContext()).apply {
            text = "Choose David AI Model"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 6)
        }.also { root.addView(it) }

        TextView(requireContext()).apply {
            text = "Your device: $ramLabel RAM  \u2022  Recommended: ${recommended.displayName}"
            textSize = 13f
            setTextColor(0xFF888888.toInt())
            setPadding(0, 0, 0, 24)
        }.also { root.addView(it) }

        // Model cards
        modelManager.allModels.forEach { model ->
            val isCompatible  = model.modelId in compatible
            val isRecommended = model.modelId == recommended.modelId

            val card = CardView(requireContext()).apply {
                radius = 16f
                cardElevation = if (isRecommended) 6f else 2f
                setCardBackgroundColor(
                    if (isRecommended) 0xFFE3F2FD.toInt()
                    else if (isCompatible) 0xFFF5F5F5.toInt()
                    else 0xFFEEEEEE.toInt()
                )
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 14) }
                layoutParams = lp
            }

            val inner = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(36, 24, 36, 20)
            }

            // Title row
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL

                TextView(requireContext()).apply {
                    text = model.displayName
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(if (isCompatible) 0xFF1565C0.toInt() else 0xFF9E9E9E.toInt())
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }.also { addView(it) }

                if (isRecommended) {
                    TextView(requireContext()).apply {
                        text = " \u2b50 Best for you"
                        textSize = 11f
                        setTextColor(0xFF1976D2.toInt())
                    }.also { addView(it) }
                }
                if (!isCompatible) {
                    TextView(requireContext()).apply {
                        text = " \u26a0\ufe0f Needs more RAM"
                        textSize = 11f
                        setTextColor(0xFFE53935.toInt())
                    }.also { addView(it) }
                }
            }.also { inner.addView(it) }

            // Description
            TextView(requireContext()).apply {
                text = model.description
                textSize = 12f
                setTextColor(0xFF616161.toInt())
                setPadding(0, 4, 0, 8)
            }.also { inner.addView(it) }

            // Size info
            TextView(requireContext()).apply {
                text = "Size: ~${modelManager.formatSize(model.estimatedBytes)}  \u2022  Min RAM: ${modelManager.formatSize(model.minRamBytes)}"
                textSize = 11f
                setTextColor(0xFF9E9E9E.toInt())
                setPadding(0, 0, 0, 10)
            }.also { inner.addView(it) }

            // Select button
            MaterialButton(requireContext()).apply {
                text = if (isRecommended) "\u2713 Use ${model.displayName}" else "Use ${model.displayName}"
                isEnabled = isCompatible
                setOnClickListener {
                    onModelSelected(model)
                    dismiss()
                }
            }.also { inner.addView(it) }

            card.addView(inner)
            root.addView(card)
        }

        return root
    }
}

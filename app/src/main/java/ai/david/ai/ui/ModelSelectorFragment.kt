package ai.david.ai.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import ai.david.ai.llm.ModelManager

class ModelSelectorFragment(
    private val modelManager: ModelManager,
    private val onModelSelected: (ModelManager.ModelInfo) -> Unit
) : BottomSheetDialogFragment() {
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val root = LinearLayout(requireContext()).apply { orientation=LinearLayout.VERTICAL; setPadding(40,40,40,60) }
        val rec = modelManager.recommendedModel()
        val compat = modelManager.compatibleModels().map { it.modelId }.toSet()
        TextView(requireContext()).apply { text="Choose David AI Model"; textSize=18f; setTypeface(null,android.graphics.Typeface.BOLD); setPadding(0,0,0,6) }.also { root.addView(it) }
        TextView(requireContext()).apply { text="Device: ${modelManager.getRamLabel()} RAM • Recommended: ${rec.displayName}"; textSize=13f; setTextColor(0xFF888888.toInt()); setPadding(0,0,0,24) }.also { root.addView(it) }
        modelManager.allModels.forEach { model ->
            val isCompat = model.modelId in compat
            val isRec    = model.modelId == rec.modelId
            val card = CardView(requireContext()).apply {
                radius=16f; cardElevation=if(isRec)6f else 2f
                setCardBackgroundColor(if(isRec)0xFFE3F2FD.toInt() else if(isCompat)0xFFF5F5F5.toInt() else 0xFFEEEEEE.toInt())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0,0,0,14) }
            }
            val inner = LinearLayout(requireContext()).apply { orientation=LinearLayout.VERTICAL; setPadding(36,24,36,20) }
            LinearLayout(requireContext()).apply {
                orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL
                TextView(requireContext()).apply { text=model.displayName; textSize=16f; setTypeface(null,android.graphics.Typeface.BOLD); setTextColor(if(isCompat)0xFF1565C0.toInt() else 0xFF9E9E9E.toInt()); layoutParams=LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f) }.also { addView(it) }
                if(isRec) TextView(requireContext()).apply { text=" ⭐ Best for you"; textSize=11f; setTextColor(0xFF1976D2.toInt()) }.also { addView(it) }
                if(!isCompat) TextView(requireContext()).apply { text=" ⚠️ Needs more RAM"; textSize=11f; setTextColor(0xFFE53935.toInt()) }.also { addView(it) }
            }.also { inner.addView(it) }
            TextView(requireContext()).apply { text=model.description; textSize=12f; setTextColor(0xFF616161.toInt()); setPadding(0,4,0,8) }.also { inner.addView(it) }
            TextView(requireContext()).apply { text="Size: ~${modelManager.formatSize(model.estimatedBytes)}  •  Min RAM: ${modelManager.formatSize(model.minRamBytes)}"; textSize=11f; setTextColor(0xFF9E9E9E.toInt()); setPadding(0,0,0,10) }.also { inner.addView(it) }
            MaterialButton(requireContext()).apply { text=if(isRec)"✓ Use ${model.displayName}" else "Use ${model.displayName}"; isEnabled=isCompat; setOnClickListener { onModelSelected(model); dismiss() } }.also { inner.addView(it) }
            card.addView(inner); root.addView(card)
        }
        return root
    }
}

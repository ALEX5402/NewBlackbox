package top.niunaijun.blackboxa.view.apps

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.util.Log

class AppsTouchCallBack(private val onMoveBlock: (from: Int, to: Int) -> Unit) :
    ItemTouchHelper.Callback() {

    companion object {
        private const val TAG = "AppsTouchCallBack"
    }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return try {
            makeMovementFlags(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN or 
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 
                0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting movement flags: ${e.message}")
            // Return safe default flags
            makeMovementFlags(0, 0)
        }
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return try {
            val fromPosition = viewHolder.bindingAdapterPosition
            val toPosition = target.bindingAdapterPosition
            
            // Validate positions
            if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
                Log.w(TAG, "Invalid positions: from=$fromPosition, to=$toPosition")
                false
            } else if (fromPosition == toPosition) {
                // Prevent moving to the same position
                false
            } else {
                // Call the move callback
                onMoveBlock(fromPosition, toPosition)
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onMove: ${e.message}")
            false
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Not implemented for this use case - drag and drop only
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        try {
            super.onSelectedChanged(viewHolder, actionState)
            
            when (actionState) {
                ItemTouchHelper.ACTION_STATE_DRAG -> {
                    // Item is being dragged
                    viewHolder?.itemView?.alpha = 0.8f
                }
                ItemTouchHelper.ACTION_STATE_IDLE -> {
                    // Item is no longer being dragged
                    viewHolder?.itemView?.alpha = 1.0f
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onSelectedChanged: ${e.message}")
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        try {
            super.clearView(recyclerView, viewHolder)
            // Reset the view to its normal state
            viewHolder.itemView.alpha = 1.0f
        } catch (e: Exception) {
            Log.e(TAG, "Error in clearView: ${e.message}")
        }
    }

    override fun canDropOver(
        recyclerView: RecyclerView,
        current: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return try {
            // Only allow dropping on valid targets
            val targetPosition = target.bindingAdapterPosition
            targetPosition != RecyclerView.NO_POSITION
        } catch (e: Exception) {
            Log.e(TAG, "Error in canDropOver: ${e.message}")
            false
        }
    }
}
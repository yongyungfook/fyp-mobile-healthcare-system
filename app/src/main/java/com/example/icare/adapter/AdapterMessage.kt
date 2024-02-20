    package com.example.icare.adapter

    import android.content.Context
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.TextView
    import androidx.recyclerview.widget.RecyclerView
    import com.example.icare.R
    import com.example.icare.model.ModelMessage
    import com.google.firebase.auth.FirebaseAuth
    import java.text.SimpleDateFormat

    class AdapterMessage(val context: Context, var messageList: ArrayList<ModelMessage>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        val ITEM_RECEIVE = 1;
        val ITEM_SENT = 2;

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            if(viewType == 1) {
                val view: View = LayoutInflater.from(context).inflate(R.layout.row_message_received, parent, false)
                return ReceiveViewHolder(view)
            } else {
                val view: View = LayoutInflater.from(context).inflate(R.layout.row_message_sent, parent, false)
                return SentViewHolder(view)
            }
        }

        override fun getItemCount(): Int {
            return messageList.size
        }

        override fun getItemViewType(position: Int): Int {
            val currentMessage = messageList[position]

            if(FirebaseAuth.getInstance().currentUser?.uid.equals(currentMessage.senderId))  {
                return ITEM_SENT
            } else {
                return ITEM_RECEIVE
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val currentMessage = messageList[position]

            if (holder.itemViewType == ITEM_SENT) {
                val viewHolder = holder as SentViewHolder
                viewHolder.sentMessage.text = currentMessage.message

                val date = java.util.Date(currentMessage.timestamp)
                val format = SimpleDateFormat("dd-MM-yyyy HH:mm")
                val sentTime = format.format(date)

                viewHolder.timeTv.text = sentTime
            } else {
                val viewHolder = holder as ReceiveViewHolder
                viewHolder.receivedMessage.text = currentMessage.message

                val date = java.util.Date(currentMessage.timestamp)
                val format = SimpleDateFormat("dd-MM-yyyy HH:mm")
                val receiveTime = format.format(date)

                viewHolder.timeTv.text = receiveTime
            }
        }

        class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val sentMessage = itemView.findViewById<TextView>(R.id.messageSentTv)
            val timeTv = itemView.findViewById<TextView>(R.id.timeTv)
        }

        class ReceiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val receivedMessage = itemView.findViewById<TextView>(R.id.messageReceivedTv)
            val timeTv = itemView.findViewById<TextView>(R.id.timeTv)
        }


    }
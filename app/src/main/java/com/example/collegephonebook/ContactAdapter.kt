package com.example.collegephonebook

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.collegephonebook.databinding.ItemContactBinding

class ContactAdapter(
    private val contacts: MutableList<Contact>,
    private val onContactClick: (Contact) -> Unit,
    private val onContactLongClick: (Contact) -> Unit,
    private val onDeleteClick: (Contact) -> Unit,
    private val onCallClick: (Contact) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    class ContactViewHolder(private val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            contact: Contact,
            onContactClick: (Contact) -> Unit,
            onContactLongClick: (Contact) -> Unit,
            onDeleteClick: (Contact) -> Unit,
            onCallClick: (Contact) -> Unit
        ) {
            binding.nameTextView.text = contact.name
            binding.numberTextView.text = contact.number
            binding.departmentTextView.text = contact.department

            itemView.setOnClickListener { onContactClick(contact) }
            itemView.setOnLongClickListener {
                onContactLongClick(contact)
                true
            }
            binding.deleteIcon.setOnClickListener { onDeleteClick(contact) }
            binding.callIcon.setOnClickListener { onCallClick(contact) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position], onContactClick, onContactLongClick, onDeleteClick, onCallClick)
    }

    override fun getItemCount() = contacts.size

    fun addContact(contact: Contact) {
        contacts.add(contact)
        notifyItemInserted(contacts.size - 1)
    }

    fun updateContact(updatedContact: Contact) {
        val position = contacts.indexOfFirst { it.id == updatedContact.id }
        if (position != -1) {
            contacts[position] = updatedContact
            notifyItemChanged(position)
        }
    }

    fun deleteContact(contact: Contact) {
        val position = contacts.indexOfFirst { it.id == contact.id }
        if (position != -1) {
            contacts.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
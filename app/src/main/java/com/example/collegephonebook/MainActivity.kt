package com.example.collegephonebook

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.telephony.SubscriptionInfo
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.collegephonebook.databinding.ActivityMainBinding
import com.example.collegephonebook.databinding.DialogContactBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var dbHelper: PhoneBookDbHelper
    private var userPhoneNumber: String? = null

    companion object {
        private const val CALL_PERMISSION_REQUEST_CODE = 1
        private const val READ_PHONE_STATE_REQUEST_CODE = 2
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Initialize database helper
            dbHelper = PhoneBookDbHelper(this)

            // Request phone state permission to get user's phone number
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), READ_PHONE_STATE_REQUEST_CODE)
            } else {
                getUserPhoneNumber()
            }

            // Setup RecyclerView
            setupRecyclerView()

            // Setup any additional click listeners from binding
            setupClickListeners()

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error initializing app", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getUserPhoneNumber() {
        try {
            val subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val subscriptionInfoList: List<SubscriptionInfo> = subscriptionManager.activeSubscriptionInfoList
            if (subscriptionInfoList.isNotEmpty()) {
                userPhoneNumber = subscriptionInfoList[0].number
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user's phone number: ${e.message}", e)
        }
    }

    private fun setupClickListeners() {
        try {
            // Only setup call icon if it exists in the layout
            binding.root.findViewById<android.widget.ImageView>(R.id.callIcon)?.setOnClickListener {
                userPhoneNumber?.let { number ->
                    makePhoneCall(number)
                } ?: run {
                    Toast.makeText(this, "User phone number not available", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up click listeners: ${e.message}", e)
        }
    }

    private fun setupRecyclerView() {
        try {
            binding.contactRecyclerView.layoutManager = LinearLayoutManager(this)

            val contacts = try {
                dbHelper.getAllContacts().toMutableList()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching contacts: ${e.message}", e)
                mutableListOf()
            }

            contactAdapter = ContactAdapter(
                contacts,
                onContactClick = { contact -> showEditContactDialog(contact) },
                onContactLongClick = { contact -> showDeleteContactDialog(contact) },
                onDeleteClick = { contact -> showDeleteContactDialog(contact) },
                onCallClick = { contact -> makePhoneCall(contact.number) }
            )
            binding.contactRecyclerView.adapter = contactAdapter

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up RecyclerView: ${e.message}", e)
            Toast.makeText(this, "Error loading contacts", Toast.LENGTH_LONG).show()
        }
    }

    private fun makePhoneCall(phoneNumber: String) {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CALL_PHONE),
                    CALL_PERMISSION_REQUEST_CODE
                )
            } else {
                startCall(phoneNumber)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error making phone call: ${e.message}", e)
            Toast.makeText(this, "Unable to make call", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCall(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting call: ${e.message}", e)
            Toast.makeText(this, "Failed to start call", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CALL_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Retry the call with the last requested number
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permission denied to make calls", Toast.LENGTH_SHORT).show()
                }
            }
            READ_PHONE_STATE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getUserPhoneNumber()
                } else {
                    Toast.makeText(this, "Permission denied to read phone state", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        try {
            menuInflater.inflate(R.menu.main_menu, menu)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating options menu: ${e.message}", e)
            return false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return try {
            when (item.itemId) {
                R.id.action_add_contact -> {
                    showAddContactDialog()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling menu item selection: ${e.message}", e)
            false
        }
    }

    private fun showAddContactDialog() {
        try {
            val dialogBinding = DialogContactBinding.inflate(layoutInflater)
            dialogBinding.countryCodeSpinner.setSelection(0) // Default to +91

            AlertDialog.Builder(this)
                .setTitle("Add Contact")
                .setView(dialogBinding.root)
                .setPositiveButton("Add") { _, _ ->
                    addContact(dialogBinding)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing add contact dialog: ${e.message}", e)
            Toast.makeText(this, "Unable to add contact", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addContact(dialogBinding: DialogContactBinding) {
        try {
            val countryCode = dialogBinding.countryCodeSpinner.selectedItem.toString()
            val number = dialogBinding.numberEditText.text.toString()
            val fullNumber = "$countryCode $number"
            val name = dialogBinding.nameEditText.text.toString()
            val department = dialogBinding.departmentEditText.text.toString()

            if (name.isBlank() || number.isBlank()) {
                Toast.makeText(this, "Name and number are required", Toast.LENGTH_SHORT).show()
                return
            }

            val id = dbHelper.addContact(name, fullNumber, department).toInt()
            if (id != -1) {
                val newContact = Contact(id, name, fullNumber, department)
                contactAdapter.addContact(newContact)
                Toast.makeText(this, "Contact added successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to add contact", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding contact: ${e.message}", e)
            Toast.makeText(this, "Error adding contact", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditContactDialog(contact: Contact) {
        try {
            val dialogBinding = DialogContactBinding.inflate(layoutInflater)
            val (countryCode, number) = contact.number.split(" ", limit = 2)
            dialogBinding.countryCodeSpinner.setSelection(getCountryCodeIndex(countryCode))
            dialogBinding.numberEditText.setText(number)
            dialogBinding.nameEditText.setText(contact.name)
            dialogBinding.departmentEditText.setText(contact.department)

            AlertDialog.Builder(this)
                .setTitle("Edit Contact")
                .setView(dialogBinding.root)
                .setPositiveButton("Save") { _, _ ->
                    updateContact(contact, dialogBinding)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing edit contact dialog: ${e.message}", e)
            Toast.makeText(this, "Unable to edit contact", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCountryCodeIndex(countryCode: String): Int {
        val countryCodes = resources.getStringArray(R.array.country_codes)
        return countryCodes.indexOf(countryCode).takeIf { it != -1 } ?: 0
    }

    private fun updateContact(contact: Contact, dialogBinding: DialogContactBinding) {
        try {
            val countryCode = dialogBinding.countryCodeSpinner.selectedItem.toString()
            val number = dialogBinding.numberEditText.text.toString()
            val fullNumber = "$countryCode $number"
            val updatedContact = contact.copy(
                name = dialogBinding.nameEditText.text.toString(),
                number = fullNumber,
                department = dialogBinding.departmentEditText.text.toString()
            )
            val rowsAffected = dbHelper.updateContact(updatedContact)
            if (rowsAffected > 0) {
                contactAdapter.updateContact(updatedContact)
                Toast.makeText(this, "Contact updated successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to update contact", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating contact: ${e.message}", e)
            Toast.makeText(this, "Error updating contact", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteContactDialog(contact: Contact) {
        try {
            AlertDialog.Builder(this)
                .setTitle("Delete Contact")
                .setMessage("Are you sure you want to delete ${contact.name}?")
                .setPositiveButton("Delete") { _, _ ->
                    deleteContact(contact)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing delete contact dialog: ${e.message}", e)
            Toast.makeText(this, "Unable to delete contact", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteContact(contact: Contact) {
        try {
            val rowsDeleted = dbHelper.deleteContact(contact.id)
            if (rowsDeleted > 0) {
                contactAdapter.deleteContact(contact)
                Toast.makeText(this, "Contact deleted successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to delete contact", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting contact: ${e.message}", e)
            Toast.makeText(this, "Error deleting contact", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        try {
            dbHelper.close()
            super.onDestroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy: ${e.message}", e)
        }
    }
}
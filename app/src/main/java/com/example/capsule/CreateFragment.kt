package com.example.capsule

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import java.text.DateFormat
import java.util.Calendar

class CreateFragment : Fragment(R.layout.fragment_create) {
    private var unlockDate: Long? = null
    private var photoUri: Uri? = null

    private val choosePhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            photoUri = uri
            view?.findViewById<ImageView>(R.id.photo_preview)?.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        unlockDate = savedInstanceState?.getLong("unlock_date")?.takeIf { it != 0L }
        photoUri = savedInstanceState?.getString("photo_uri")?.let(Uri::parse)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val dateButton = view.findViewById<MaterialButton>(R.id.unlock_date)
        unlockDate?.let { dateButton.text = dateLabel(it) }
        photoUri?.let { view.findViewById<ImageView>(R.id.photo_preview).setImageURI(it) }
        view.findViewById<MaterialButton>(R.id.choose_photo).setOnClickListener {
            choosePhoto.launch("image/*")
        }
        dateButton.setOnClickListener {
            val tomorrow = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val selected = Calendar.getInstance().apply {
                timeInMillis = maxOf(unlockDate ?: tomorrow.timeInMillis, tomorrow.timeInMillis)
            }
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selected.set(year, month, day)
                    unlockDate = selected.timeInMillis
                    dateButton.text = dateLabel(selected.timeInMillis)
                },
                selected.get(Calendar.YEAR),
                selected.get(Calendar.MONTH),
                selected.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.minDate = tomorrow.timeInMillis
            }.show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        unlockDate?.let { outState.putLong("unlock_date", it) }
        photoUri?.let { outState.putString("photo_uri", it.toString()) }
    }

    private fun dateLabel(date: Long): String =
        getString(R.string.unlock_date_value, DateFormat.getDateInstance().format(date))
}

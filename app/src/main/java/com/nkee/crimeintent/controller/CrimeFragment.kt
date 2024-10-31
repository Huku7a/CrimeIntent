package com.nkee.crimeintent.controller

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract.Contacts
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.nkee.crimeintent.R
import com.nkee.crimeintent.data.ContactPicker
import com.nkee.crimeintent.getScaledBitmap
import com.nkee.crimeintent.model.Crime
import java.io.File
import java.util.Date
import java.util.UUID

private const val ARG_CRIME_ID = "crime_id"
private const val TAG = "CrimeFragment"
private const val DIALOG_DATE = "DialogDate"
const val REQUEST_DATE = "RequestCrimeDate"
private const val DATE_FORMAT = "EEE, MMM, dd"

class CrimeFragment : Fragment() {
    private lateinit var crime: Crime
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var chooseSuspectButton: Button
    private lateinit var photoView: ImageView
    private val crimeDetailViewModel by lazy { ViewModelProvider(this)[CrimeDetailViewModel::class.java] }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(ARG_CRIME_ID, UUID::class.java)
        } else {
            arguments?.getSerializable(ARG_CRIME_ID) as UUID
        }
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            updatePhoto()
        }
        Log.d(TAG, "args bundle crime ID: $crimeId")
        // Загрузка преступления из базы данных
        crimeId?.let { crimeDetailViewModel.loadCrime(it) } ?: Log.w(TAG, "args bundle crime ID is null")
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)
        titleField = view.findViewById(R.id.crime_title)
        dateButton = view.findViewById(R.id.crime_date)
        solvedCheckBox = view.findViewById(R.id.crime_solved)
        reportButton = view.findViewById(R.id.crime_report)
        chooseSuspectButton = view.findViewById(R.id.crime_suspect)
        photoView = view.findViewById(R.id.crime_photo)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(viewLifecycleOwner) { crime ->
            crime?.let {
                this.crime = crime
                photoFile = crimeDetailViewModel.getPhotoFile(crime)
                photoUri = FileProvider.getUriForFile(requireActivity(), "com.nkee.crimeintent.fileProvider", photoFile)
                updateUI()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title = s.toString()
            }

            override fun afterTextChanged(s: Editable?) { }
        }
        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }

        photoView.apply {
            val packageManager = requireActivity().packageManager
            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity: ResolveInfo? = packageManager.resolveActivity(captureImage, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null) {
                isEnabled = false
            }

            setOnClickListener {
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

                val cameraActivities: List<ResolveInfo> = packageManager.queryIntentActivities(captureImage, PackageManager.MATCH_DEFAULT_ONLY)

                for (cameraActivity in cameraActivities) {
                    requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }

                cameraLauncher.launch(captureImage)
            }
        }

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply {
                this@CrimeFragment.parentFragmentManager.setFragmentResultListener(REQUEST_DATE, this@CrimeFragment.viewLifecycleOwner) { _, bundle ->
                    val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        bundle.getSerializable(REQUEST_DATE, Date::class.java)
                    } else {
                        bundle.getSerializable(REQUEST_DATE) as Date
                    }
                    onDateSelected(result as Date)
                }
                show(this@CrimeFragment.parentFragmentManager, DIALOG_DATE)
            }
        }

        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }.also { intent ->
                val chooserIntent = Intent.createChooser(intent, getString(R.string.crime_report_send))
                startActivity(chooserIntent)
            }
        }

        chooseSuspectButton.apply {
            val contactPicker = ContactPicker(requireActivity().activityResultRegistry) { uri ->
                val queryFields = arrayOf(Contacts.DISPLAY_NAME)
                val cursor = uri?.let {
                    requireActivity().contentResolver
                        .query(it, queryFields, null, null, null)
                }
                cursor?.use { c ->
                    c.moveToFirst()
                    val suspect = c.getString(0)
                    crime.suspect = suspect
                    crimeDetailViewModel.saveCrime(crime)
                    chooseSuspectButton.text = suspect
                }
            }
            setOnClickListener {
                contactPicker.pickContact()
            }

//            val packageManager: PackageManager = requireActivity().packageManager
//            val resolvedActivity: ResolveInfo? =
//                packageManager.resolveActivity(Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI), PackageManager.MATCH_DEFAULT_ONLY)
//            if (resolvedActivity == null) {
//                isEnabled = false
//            }

        }
    }

    private fun onDateSelected(date: Date) {
        crime.date = date
        updateUI()
    }

    private fun updateUI() {
        titleField.setText(crime.title)
        dateButton.text = crime.date.toString()
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
        if (crime.suspect.isNotEmpty()) {
            chooseSuspectButton.text = crime.suspect
        }
        updatePhoto()
    }

    private fun updatePhoto() {
        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path, requireActivity())
            photoView.setImageBitmap(bitmap)
        } else {
            photoView.setImageDrawable(null)
        }
    }

    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        var suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }

        return getString(R.string.crime_report, crime.title, dateString, solvedString, suspect)
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }

            val fragment = CrimeFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
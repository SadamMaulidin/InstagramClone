package id.cikup.instagramclone.ui.profile.setting

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.squareup.picasso.Picasso
import com.theartofdev.edmodo.cropper.CropImage
import cookode.instagram_clone.R
import id.cikup.instagramclone.data.model.User
import kotlinx.android.synthetic.main.fragment_setting.*


class SettingFragment : Fragment() {


    private lateinit var firebaseUser: FirebaseUser
    private var cekInfoProfile = ""
    private var myUrl = ""
    private var imageUri : Uri? = null
    private var storageProfilePictureRef: StorageReference? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setting, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        firebaseUser = FirebaseAuth.getInstance().currentUser!!
        storageProfilePictureRef = FirebaseStorage.getInstance().reference.child("Profile Picture")

        logout_btn_setprofile.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
           findNavController().navigate(R.id.action_settingFragment_to_mainActivity)
        }

        change_setimage_text.setOnClickListener {
            cekInfoProfile = "clicked"

            CropImage.activity()
                .setAspectRatio(1,1)
                .start(this.requireActivity())
        }

        save_info_profile_btn.setOnClickListener {
            if (cekInfoProfile == "clicked"){
                //create method untuk upload image profile
                uploadImageAndUpdateInfo()
            } else {
                updateUserInfoOnly()
            }

        }
        userInfo()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK
            && data!= null){
            val result = CropImage.getActivityResult(data)
            imageUri = result.uri
            setprofile_image_view.setImageURI(imageUri)
        } else {

        }
    }

    private fun updateUserInfoOnly() {
        //after create user map
        when {
            TextUtils.isEmpty(fullname_setprofile_edittext.text.toString()) -> {
                Toast.makeText(this.requireContext(),"Please dont be empty..", Toast.LENGTH_LONG).show()
            }
            username_setprofile_edittext.text.toString() == "" -> {
                Toast.makeText(this.requireContext(),"Please dont be empty..", Toast.LENGTH_LONG).show()
            }
            bio_setprofile_edittext.text.toString() == "" -> {
                Toast.makeText(this.requireContext(),"Please dont be empty..", Toast.LENGTH_LONG).show()
            }
            else -> {
                val usersRef = FirebaseDatabase.getInstance().reference
                    .child("Users")

                val userMap = HashMap<String, Any>()
                userMap["fullname"] = fullname_setprofile_edittext.text.toString().toLowerCase()
                userMap["username"] = username_setprofile_edittext.text.toString().toLowerCase()
                userMap["bio"]      = bio_setprofile_edittext.text.toString().toLowerCase()

                usersRef.child(firebaseUser.uid).updateChildren(userMap)

                Toast.makeText(this.requireContext(),"Info Profile has been update", Toast.LENGTH_LONG).show()

                findNavController().navigate(R.id.action_settingFragment_to_homeActivity2)

            }
        }

    }

    private fun userInfo(){
        val usersRef = FirebaseDatabase.getInstance().getReference()
            .child("Users").child(firebaseUser.uid)

        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {

                if (p0.exists()){
                    val user = p0.getValue<User>(User::class.java)

                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile)
                        .into(setprofile_image_view)
                    username_setprofile_edittext.setText(user.getUsername())
                    fullname_setprofile_edittext.setText(user.getFullname())
                    bio_setprofile_edittext.setText(user.getBio())
                }
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })
    }

    private fun uploadImageAndUpdateInfo() {

        when{
            imageUri == null -> Toast.makeText(this.requireContext(),"Please select image", Toast.LENGTH_LONG).show()
            TextUtils.isEmpty(fullname_setprofile_edittext.text.toString()) -> {
                Toast.makeText(this.requireContext(),"Please dont be empty..", Toast.LENGTH_LONG).show()
            }
            username_setprofile_edittext.text.toString() == "" -> {
                Toast.makeText(this.requireContext(),"Please dont be empty..", Toast.LENGTH_LONG).show()
            }
            bio_setprofile_edittext.text.toString() == "" -> {
                Toast.makeText(this.requireContext(),"Please dont be empty..", Toast.LENGTH_LONG).show()
            }
            else -> {
                val progressDialog = ProgressDialog(this.requireContext())
                progressDialog.setTitle("ACCOUNT SETTING")
                progressDialog.setMessage("Please wait.., we are updating profile..")
                progressDialog.show()

                val fileRef = storageProfilePictureRef!!.child(firebaseUser!!.uid + "jpg")

                var uploadTask: StorageTask<*>
                uploadTask = fileRef.putFile(imageUri!!)
                uploadTask.continueWithTask(Continuation <UploadTask.TaskSnapshot, Task<Uri>>{ task ->
                    if (!task.isSuccessful){

                        task.exception.let {
                            throw it!!
                            progressDialog.dismiss()
                        }
                    }
                    return@Continuation fileRef.downloadUrl
                }).addOnCompleteListener ( OnCompleteListener<Uri> { task ->
                    if (task.isSuccessful){
                        val downloadUrl = task.result
                        myUrl = downloadUrl.toString()

                        val ref = FirebaseDatabase.getInstance().reference.child("Users")

                        val userMap = HashMap<String, Any>()
                        //sesuai dengan Firebase Database
                        userMap["fullname"] = fullname_setprofile_edittext.text.toString().toLowerCase()
                        userMap["username"] = username_setprofile_edittext.text.toString().toLowerCase()
                        userMap["bio"]      = bio_setprofile_edittext.text.toString().toLowerCase()
                        userMap["image"]    = myUrl

                        ref.child(firebaseUser.uid).updateChildren(userMap)

                        Toast.makeText(this.requireContext(),"Info Profile has been update", Toast.LENGTH_LONG).show()

                        findNavController().navigate(R.id.action_settingFragment_to_homeActivity2)
//                        val intent = Intent(this@SettingAccountActivity, MainActivity::class.java)
//                        startActivity(intent)
//                        finish()
                        progressDialog.dismiss()
                    } else {
                        progressDialog.dismiss()
                    }
                })
            }

        }
    }

}
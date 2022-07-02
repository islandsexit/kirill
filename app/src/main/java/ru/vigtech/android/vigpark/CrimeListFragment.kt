package ru.vigtech.android.vigpark

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Base64
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.FONT_HERSHEY_SIMPLEX
import org.opencv.objdetect.CascadeClassifier
import org.opencv.utils.Converters
import ru.vigtech.android.vigpark.api.ApiClient
import java.io.*
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*


private const val TAG = "CrimeListFragment"
private const val SAVED_SUBTITLE_VISIBLE = "subtitle"
private const val PICK_IMAGE = 1

class CrimeListFragment : Fragment(), CameraBridgeViewBase.CvCameraViewListener2 {



    private lateinit var mRgba:Mat
    private lateinit var mGray:Mat
    public lateinit var cameraBridgeViewBase: myCameraView
    var img64_full: String? = null
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    var baseLoaderCallback: BaseLoaderCallback? = null
    private val FACE_RECT_COLOR = Scalar(0.0, 255.0, 0.0, 255.0)
    private lateinit var mCascadeFile: File
    private var mJavaDetector: CascadeClassifier? = null
    private val mRelativeFaceSize = 0.1f
    private var mAbsoluteFaceSize = 0
    private var count = 0

    private var isScaner = false
    private var flashEnable = false
    private lateinit var menu: Menu

    private val TIMER_DURATION = 2000L
    private val TIMER_INTERVAL = 100L

    private lateinit var mCountDownTimer: CountDownTimer
    private lateinit var mCountDownTimer2: CountDownTimer
    private var can_take_photo = false



    private lateinit var crimeRecyclerView: RecyclerView
    private lateinit var img_file: File
    private lateinit var mybitmap: Bitmap

    private lateinit var photoButton:ImageButton

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var actionBarToggle: ActionBarDrawerToggle
    private lateinit var navView: NavigationView

    //Network dnn
    private lateinit var proto: String
    private lateinit var weights: String
    private lateinit var net: Net


    private var adapter: CrimeAdapter? = CrimeAdapter(emptyList())
    val crimeListViewModel: CrimeListViewModel by lazy {
        ViewModelProviders.of(this).get(CrimeListViewModel::class.java)
    }



    private var callbacks: Callbacks? = null


    interface Callbacks {
        fun onCrimeSelected(crimeId: UUID)
    }






    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as? Callbacks

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {



        val view = inflater.inflate(R.layout.fragment_crime_list, container, false)

        drawerLayout = view.findViewById(R.id.drawer_layout)


        actionBarToggle = ActionBarDrawerToggle(activity, drawerLayout, 0, 0)
        drawerLayout.addDrawerListener(actionBarToggle)

        actionBarToggle.syncState()



        // Call findViewById on the NavigationView
        navView = view.findViewById(R.id.navigation)

        crimeRecyclerView =
                view.findViewById(R.id.crime_recycler_view) as RecyclerView
        crimeRecyclerView.layoutManager = LinearLayoutManager(context)
        photoButton = view.findViewById(R.id.camera_button) as ImageButton

        photoButton.setOnClickListener {
            try {
                cameraBridgeViewBase.takePicture(UUID.randomUUID().toString())
            }
            catch (e: Exception){
                Log.e("PictureDemo", "Exception in take photo", e)
            }

        }

        //        PopUpClass popUp = new PopUpClass();
//        popUp.showPopupWindow(this);
        can_take_photo = true
        mCountDownTimer =
            object : CountDownTimer(TIMER_DURATION, TIMER_INTERVAL) {
                override fun onTick(millisUntilFinished: Long) {
                    can_take_photo = false
                }

                override fun onFinish() {
                    count = 0
                    can_take_photo = true

                }
            }.start()
        mCountDownTimer2 =
            object : CountDownTimer(TIMER_DURATION, TIMER_INTERVAL) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    count = 0

                }
            }.start()


        cameraBridgeViewBase = view.findViewById(R.id.myCameraView) as myCameraView
//        cameraBridgeViewBase.setVisibility(View.VISIBLE);
        //        cameraBridgeViewBase.setVisibility(View.VISIBLE);
        cameraBridgeViewBase.setCameraIndex(0)

        cameraBridgeViewBase.setCvCameraViewListener(this)
        cameraBridgeViewBase.setMaxFrameSize(1280, 720)

        baseLoaderCallback = object : BaseLoaderCallback(context) {
            override fun onManagerConnected(status: Int) {
                super.onManagerConnected(status)
                when (status) {
                    SUCCESS -> {
                        run {
                            Log.i(TAG, "OpenCV loaded successfully")

                            // Load native library after(!) OpenCV initialization
//                        System.loadLibrary("ndklibrarysample");
                            try {
                                // load cascade file from application resources
                                val `is` = resources.openRawResource(R.raw.haarcascade_russian_plate_number)
                                val cascadeDir: File? = context?.getDir("cascade", Context.MODE_PRIVATE)
                                mCascadeFile = File(cascadeDir, "haarcascade_frontalface_alt2.xml")
                                val os = FileOutputStream(mCascadeFile)
                                val buffer = ByteArray(4096)
                                var bytesRead: Int
                                while (`is`.read(buffer).also { bytesRead = it } != -1) {
                                    os.write(buffer, 0, bytesRead)
                                }
                                `is`.close()
                                os.close()
                                mJavaDetector = CascadeClassifier(mCascadeFile.getAbsolutePath())
                                if (mJavaDetector!!.empty()) {
                                    Log.e(
                                        TAG,
                                        "Failed to load cascade classifier"
                                    )
                                    mJavaDetector = null
                                } else Log.i(
                                    TAG,
                                    "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath()
                                )
                                cascadeDir?.delete()
                            } catch (e: IOException) {
                                e.printStackTrace()
                                Log.e(
                                    TAG,
                                    "Failed to load cascade. Exception thrown: $e"
                                )
                            }
                            checkOpenCV(context);
                            cameraBridgeViewBase.enableView()
                        }
                        super.onManagerConnected(status)
                    }
                    else -> super.onManagerConnected(status)
                }
            }
        }


        //todo свайп
        val swipeHandler = object : SwipeToDeleteCallback(this.context) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = crimeRecyclerView.adapter
                CoroutineScope(Dispatchers.Default).launch{
                    val crime = crimeListViewModel.getCrimeFromPosition(viewHolder.adapterPosition)
                    crimeListViewModel.deleteCrime(crime)
                   val file = File(crime.img_path)
                    if(file.exists()){
                        file.delete()
                    }
                    Log.i("AAAAAAAAAAPPPPPP_TAAAGGG", "Deleting ${crime.title}")
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(crimeRecyclerView)

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.plate_selector -> {
                    if (!isScaner) {
                        isScaner = true
                        menuItem.setIcon(R.drawable.ic_scaner)
                        menuItem.setTitle("Сканер")
                        photoButton.visibility = View.GONE
                    }
                    else{
                        isScaner = false
                        menuItem.setIcon(R.drawable.ic_cam)
                        menuItem.setTitle("Фото")
                        photoButton.visibility = View.VISIBLE
                    }
                    true
                }
                R.id.new_crime -> {
                    pickPhoto()
                    true
                }

                R.id.delete_crimes -> {
                    val builder1: AlertDialog.Builder = AlertDialog.Builder(requireContext())
                    builder1.setMessage("Удалить все снимки?")
                    builder1.setCancelable(true)

                    builder1.setPositiveButton(
                        "Да",
                        DialogInterface.OnClickListener { dialog, id ->
                            try {
                                val lastCrimeDate = crimeListViewModel.crimeListLiveData.value?.first()?.date
                                crimeListViewModel.crimeListLiveData.observe(viewLifecycleOwner, Observer<List<Crime>>(){
                                    for(crime in it) {
                                        CoroutineScope(Dispatchers.Default).launch {
                                            if (crime.date <= lastCrimeDate) {
                                                val file = File(crime.img_path)
                                                if (file.exists()) {
                                                    file.delete()
                                                    crimeListViewModel.deleteCrime(crime)
                                                } else {
                                                    this.cancel()
                                                }
                                            }
                                        }


                                    }})
                            } catch (e: Exception){
                                Toast.makeText(requireContext(), "Ничего не нашлось", Toast.LENGTH_SHORT).show()
                            }
                            dialog.cancel() })

                    builder1.setNegativeButton(
                        "Нет",
                        DialogInterface.OnClickListener { dialog, id ->

                            dialog.cancel() })

                    val alert11: AlertDialog = builder1.create()
                    alert11.show()


                    true
                }
                else -> {
                    false
                }
            }

        }

        return view
    }

    override fun onResume() {
        super.onResume()


        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(context, "There's a problem, yo!", Toast.LENGTH_SHORT)
                .show()
        } else {
            baseLoaderCallback!!.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        this.menu = menu
        inflater.inflate(R.menu.fragment_crime_list, menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var count = 0
        return when (item.itemId) {

            R.id.flash ->{
                if(flashEnable){
                    cameraBridgeViewBase.flash(false)
                    item.setIcon(R.drawable.ic_flash_off)
                    flashEnable = false
                }
                else{
                    cameraBridgeViewBase.flash(true)
                    item.setIcon(R.drawable.ic_flash_on)
                    flashEnable = true
                }



                true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    val selectImageFromGalleryResult = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            //todo image picker

            Log.i("Image_picker", uri.path.toString())
            if(it != null) {
                try {
                    var path_to_image = context?.filesDir
                    val inputStream: InputStream? =
                        context?.getContentResolver()?.openInputStream(uri)
                    val bOut2 = ByteArrayOutputStream()
                    var bm = BitmapFactory.decodeStream(inputStream)
                    bm = PicturesUtils.getResizedBitmap(bm, 720, 480)
                    bm.compress(Bitmap.CompressFormat.JPEG, 50, bOut2)
                    img64_full = Base64.encodeToString(bOut2.toByteArray(), Base64.DEFAULT)
                    val mFile3 = File(
                        path_to_image,
                        UUID.randomUUID().toString() + "_" + ".jpg"
                    )
                    var fos2: FileOutputStream? = null
                    fos2 = FileOutputStream(mFile3)
                    Log.i("APP_LOG", mFile3.absolutePath)
                    bm.compress(Bitmap.CompressFormat.JPEG, 100, fos2)
                    val bOut = ByteArrayOutputStream()
                    bm.compress(Bitmap.CompressFormat.JPEG, 100, bOut)
                    val img64 = Base64.encodeToString(bOut.toByteArray(), Base64.DEFAULT)
                    ApiClient.POST_img64(
                        img64_full.toString(),
                        "http://192.168.48.174:8080/",
                        img_path = mFile3.path,
                        img_plate_path = "None"
                    )
                } catch (e: IOException) {
                    Log.e("APP_LOG", "Exception in photoCallback", e)
                }
            }

        }
    }

    private fun pickPhoto() {
//        val pickPhotoIntent = Intent()
//        pickPhotoIntent.setType("image/*")
//        pickPhotoIntent.setAction(Intent.ACTION_GET_CONTENT)
//        startActivityForResult(Intent.createChooser(pickPhotoIntent,"Выбери откуда взять фото"), PICK_IMAGE)

          selectImageFromGalleryResult.launch("image/*")


    }

    private fun updateUI(crimes: List<Crime>) {
        adapter?.let {
            it.crimes = crimes
        } ?: run {
            adapter = CrimeAdapter(crimes)
        }
        crimeRecyclerView.adapter = adapter
    }

    private inner class CrimeHolder(view: View)
        : RecyclerView.ViewHolder(view), View.OnClickListener {

        private lateinit var crime: Crime

        private val titleTextView: TextView = itemView.findViewById(R.id.crime_title)
        private val dateTextView: TextView = itemView.findViewById(R.id.crime_date)
        private val solvedImageView: ImageView = itemView.findViewById(R.id.crime_solved)
        private val sendIcon:ImageView = itemView.findViewById(R.id.connection)
        private val foundIcon:ImageView = itemView.findViewById(R.id.not_found)



        init {
            itemView.setOnClickListener(this)

        }

        @SuppressLint("SimpleDateFormat")
        fun bind(crime: Crime) {
            this.crime = crime
            titleTextView.text = this.crime.title
            dateTextView.text = SimpleDateFormat("dd-MM-yyyy HH:mm").format(this.crime.date)
//            var path_to_image = "/storage/emulated/0/${crime.img_path}"
            if(File(crime.img_path).exists() && crime.img_path != ""){
                mybitmap = BitmapFactory.decodeFile(crime.img_path)
                solvedImageView.setImageBitmap(Bitmap.createScaledBitmap(mybitmap, 120, 120, false))
                solvedImageView.setVisibility(View.VISIBLE)
            }else{
                solvedImageView.setVisibility(View.INVISIBLE)
            }
            if(!crime.send){
                sendIcon.setVisibility(View.VISIBLE)
                foundIcon.setVisibility(View.GONE)

            }
            else{
                sendIcon.setVisibility(View.GONE)
                if(!crime.found){
                    foundIcon.setVisibility(View.VISIBLE)
                }
                else{
                    foundIcon.setVisibility(View.GONE)
                }
            }






        }

        override fun onClick(v: View) {
            callbacks?.onCrimeSelected(crime.id)
//            if (crime.send) {
//                callbacks?.onCrimeSelected(crime.id)
//            }
//            else{
//               lifecycleScope.launch {
//                   delay(3000)
//                   ResendCrime(crime)
//               }
//            }
        }
    }

    private inner class CrimeAdapter(var crimes: List<Crime>)
        : RecyclerView.Adapter<CrimeHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
                : CrimeHolder {
            val layoutInflater = LayoutInflater.from(context)
            val view = layoutInflater.inflate(R.layout.list_item_crime, parent, false)
            return CrimeHolder(view)
        }


        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            val crime = crimes[position]
            holder.bind(crime)
        }
        fun removeAt(position: Int) {
//            crimes.removeAt(position) //todo remove adapter
            notifyItemRemoved(position)
        }

        override fun getItemCount() = crimes.size
    }

    companion object{

        fun newInstance(): CrimeListFragment {
            return CrimeListFragment()
        }
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mGray = Mat()
        mRgba = Mat() }

    override fun onCameraViewStopped() {
        mGray.release()
        mRgba.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {


        mRgba = inputFrame.rgba()
        // mGray = inputFrame.gray();
        // mGray = inputFrame.gray();
        mGray = rotateMat(inputFrame.gray())

        if (mAbsoluteFaceSize == 0) {
            val height = mGray.rows()
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize)
            }
//            mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        val faces = MatOfRect()

        if (true) {
            if (true) mJavaDetector!!.detectMultiScale(
                mGray, faces, 1.1, 2, 2,  // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                Size(mAbsoluteFaceSize.toDouble(), mAbsoluteFaceSize.toDouble()), Size()
            )
        } else {
            Log.e(TAG, "Detection method is not selected!")
        }
        mGray.release()
        val newMat: Mat = rotateMat(mRgba)

        val facesArray = faces.toArray()
        for (i in facesArray.indices) {
            Imgproc.rectangle(
                newMat,
                facesArray[i].tl(),
                facesArray[i].br(),
                FACE_RECT_COLOR,
                3
            )
            Log.i("MainActivity.this", "x:" + facesArray[0].x)
            Log.i("MainActivity.this", "y:" + facesArray[0].y)
            Log.i("MainActivity.this", "w:" + facesArray[0].width)
            Log.i("MainActivity.this", "h:" + facesArray[0].height)
            Log.e(TAG, "plate detected!")
            if (isScaner) {
                count++
                if (can_take_photo) {
                    when (count) {
                        50 -> {
                            Log.i(
                                TAG,
                                "toast wait---------------------------------------------------------------------------------------------------"
                            )

                            mCountDownTimer2.cancel()
                            mCountDownTimer2.start()
                            count++
                        }
                        55 -> {
                            cameraBridgeViewBase.setFace_array(facesArray)
                            val uuid = UUID.randomUUID().toString() + ".png"
                            cameraBridgeViewBase.takePicture(uuid)
                            mCountDownTimer.cancel()
                            mCountDownTimer.start()
                            count = 0
                        }
                        else -> {}
                    }
//                //                if (count == 10) {
////
////                    cameraBridgeViewBase.setFace_array(facesArray);
////                    String uuid = UUID.randomUUID().toString() + ".png";
////                    cameraBridgeViewBase.takePicture(uuid);
////                    mCountDownTimer.start();
////                    count=0;
////
////
////
////                }
                }
            }
        }
//
//

        Imgproc.resize(
            newMat, mRgba, Size(
                mRgba.width().toDouble(),
                mRgba.height().toDouble()
            )
        )
        newMat.release()

        return mRgba;
    }



private fun checkOpenCV(context: Context?) {
        if (OpenCVLoader.initDebug()) {
            //shortMsg(context, OPENCV_SUCCESSFUL)
            cameraBridgeViewBase.let {
                cameraBridgeViewBase.setCameraPermissionGranted()
                cameraBridgeViewBase.enableView()

            }
        } else {Log.i(TAG, "Problem with check Opencv") }
    }


    fun rotateMat(matImage: Mat): Mat {
        val rotated = matImage.t()
        Core.flip(rotated, rotated, 1)
        return rotated
    }


    override fun onDestroy() {
        super.onDestroy()
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView()
        }
    }


    override fun onPause() {
        super.onPause()
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView()
        }
        try {
            flashEnable = false
            menu.findItem(R.id.flash).setIcon(R.drawable.ic_flash_off)
            isScaner = false
        }
        catch (e:Exception){
            //todo onpause crrush fix first launch
        }
    }

    override fun onStart() {
        super.onStart()
        crimeListViewModel.crimeListLiveData.observe(
            viewLifecycleOwner,
            Observer { crimes ->
                crimes?.let {
                    Log.i(TAG, "Got crimeLiveData ${crimes.size}")
                    updateUI(crimes)
                }
            }
        )
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }


    suspend fun ResendCrime(crime: Crime){
        //todo удалить строку, на которую нажал и коорая не отправилась
        if(File(crime.img_path_full).exists() && crime.img_path_full != "") {
            val bOut2 = ByteArrayOutputStream()
            var bm = BitmapFactory.decodeFile(crime.img_path_full)
            bm.compress(Bitmap.CompressFormat.JPEG, 100, bOut2)
            img64_full = Base64.encodeToString(bOut2.toByteArray(), Base64.DEFAULT)

            ApiClient.POST_img64(img64_full.toString(), "i", crime)
        }


    }



}



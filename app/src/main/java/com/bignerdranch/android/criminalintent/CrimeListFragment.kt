package com.bignerdranch.android.criminalintent

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

private const val TAG = "CrimeListFragment"
private const val SAVED_SUBTITLE_VISIBLE = "subtitle"

class CrimeListFragment : Fragment(), CameraBridgeViewBase.CvCameraViewListener2 {



    private lateinit var mRgba:Mat
    private lateinit var mGray:Mat
    private lateinit var cameraBridgeViewBase: myCameraView

    var baseLoaderCallback: BaseLoaderCallback? = null
    private val FACE_RECT_COLOR = Scalar(0.0, 255.0, 0.0, 255.0)
    private lateinit var mCascadeFile: File
    private var mJavaDetector: CascadeClassifier? = null
    private val mRelativeFaceSize = 0.1f
    private var mAbsoluteFaceSize = 0
    private var count = 0

    private val TIMER_DURATION = 2000L
    private val TIMER_INTERVAL = 100L

    private lateinit var mCountDownTimer: CountDownTimer
    private lateinit var mCountDownTimer2: CountDownTimer
    private val can_take_photo = false


    private lateinit var crimeRecyclerView: RecyclerView
    private lateinit var img_file: File
    private lateinit var mybitmap: Bitmap
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

        crimeRecyclerView =
                view.findViewById(R.id.crime_recycler_view) as RecyclerView
        crimeRecyclerView.layoutManager = LinearLayoutManager(context)


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
        inflater.inflate(R.menu.fragment_crime_list, menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_crime -> {
                val crime = Crime()
                crimeListViewModel.addCrime(crime)
                callbacks?.onCrimeSelected(crime.id)
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
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

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(crime: Crime) {
            this.crime = crime
            titleTextView.text = this.crime.title
            dateTextView.text = this.crime.date.toString()
            var path_to_image = "/storage/emulated/0/${crime.img_path}"
            if(File(path_to_image).exists() && crime.img_path != ""){
                mybitmap = BitmapFactory.decodeFile(path_to_image)
                solvedImageView.setImageBitmap(Bitmap.createScaledBitmap(mybitmap, 120, 120, false))
                solvedImageView.setVisibility(View.VISIBLE)
            }else{
                solvedImageView.setVisibility(View.INVISIBLE)
            }





        }

        override fun onClick(v: View) {
            callbacks?.onCrimeSelected(crime.id)
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

        override fun getItemCount() = crimes.size
    }

    companion object {
        fun newInstance(): CrimeListFragment {
            return CrimeListFragment()
        }
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mGray = Mat()
        mRgba = Mat()    }

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
                    99 -> {
                        cameraBridgeViewBase.setFace_array(facesArray)
                        val uuid = UUID.randomUUID().toString() + ".png"
                        cameraBridgeViewBase.takePicture(uuid)
                        mCountDownTimer.cancel()
                        mCountDownTimer.start()
                        count = 0
                    }
                    else -> {}
                }
                //                if (count == 10) {
//
//                    cameraBridgeViewBase.setFace_array(facesArray);
//                    String uuid = UUID.randomUUID().toString() + ".png";
//                    cameraBridgeViewBase.takePicture(uuid);
//                    mCountDownTimer.start();
//                    count=0;
//
//
//
//                }
            }
        }


//            cameraBridgeViewBase.takePicture("test1.jpg");
        //TODO face detected
        //Intent intent = new Intent(ScreenSaver.this, MainActivity.class);
        // startActivity(intent);


//            cameraBridgeViewBase.takePicture("test1.jpg");
        //TODO face detected
        //Intent intent = new Intent(ScreenSaver.this, MainActivity.class);
        // startActivity(intent);
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
}
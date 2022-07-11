package ru.vigtech.android.vigpark

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.util.Base64
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.google.android.gms.cast.CastRemoteDisplayLocalService.startService
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.vigtech.android.vigpark.api.ApiClient
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


private const val TAG = "CrimeListFragment"

class CrimeListFragment : Fragment(),
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener,
    com.google.android.gms.location.LocationListener{

    private var mGoogleApiClient: GoogleApiClient? = null
    private var mLocation: Location? = null
    private var mLocationManager: LocationManager? = null

    private var mLocationRequest: LocationRequest? = null
    private val listener: com.google.android.gms.location.LocationListener? = null
    private val UPDATE_INTERVAL = (2 * 1000).toLong()  /* 10 secs */
    private val FASTEST_INTERVAL: Long = 2000 /* 2 sec */

    var latLng: LatLng = LatLng(0.0, 0.0)

    private val REQUEST_PERMISSIONS = 100
    var boolean_permission = false

    private var locationManager: LocationManager? = null

    private val isLocationEnabled: Boolean
        get() {
            locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }




    lateinit var cameraxHelper: CameraxHelper


    var img64_full: String? = null

    private lateinit var menu: Menu



    private lateinit var crimeRecyclerView: RecyclerView
    private lateinit var mybitmap: Bitmap

    private lateinit var photoButton:ImageButton


    private lateinit var drawerLayout: DrawerLayout
    private lateinit var actionBarToggle: ActionBarDrawerToggle
    private lateinit var navView: NavigationView
    var zone: Int = 0



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









        cameraxHelper = CameraxHelper(
            caller = this,
            previewView =  view.findViewById(R.id.previewView),
            onPictureTaken = { file, uri ->
                Log.i("apptg", "Picture taken ${file.absolutePath} uri=$uri")
            },
            onError = { Log.e("APPTAG", "error") },
            builderPreview = Preview.Builder().setTargetResolution(android.util.Size(200,200)),
            builderImageCapture = ImageCapture.Builder().setTargetResolution(android.util.Size(1024,768)),
            filesDirectory = context?.filesDir,
            latLng = latLng

        )
        cameraxHelper.start()
        cameraxHelper.changeCamera()

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
                cameraxHelper.takePicture()
            }
            catch (e: Exception){
                Log.e("PictureDemo", "Exception in take photo", e)
            }

        }






       //todo camera size cameraBridgeViewBase.setMaxFrameSize(1280, 720)






        //todo свайп
        val swipeHandler = object : SwipeToDeleteCallback(this.context) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                crimeRecyclerView.adapter?.notifyItemRemoved(viewHolder.position)
                CoroutineScope(Dispatchers.Default).launch {
                   val crime = crimeListViewModel.getCrimeFromPosition(viewHolder.position)

                    CrimeRepository.get().deleteCrime(crime)
                    //notifyItemRemoved(position) execute only once
                    //crimeListViewModel.deleteCrime(crime)
//                    crimeListViewModel.deleteCrime(crime)
                    val file = File(crime.img_path)
                    if (file.exists()) {
                        file.delete()
                    }
                    Log.i("AAAAAAAAAAPPPPPP_TAAAGGG", "Deleting ${viewHolder.position}")
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(crimeRecyclerView)

//todo resend swipe
        val swipeHandlerResend = object : SwipeToResendCallback(this.context) {
            @SuppressLint("NotifyDataSetChanged")
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                CoroutineScope(Dispatchers.Default).launch {
                    val crime = crimeListViewModel.getCrimeFromPosition(viewHolder.adapterPosition)
                    ResendCrime(crime)
                    Log.i("Swipe Resend", "Resend ${crime.title}")
                }
            }
        }
        val itemTouchHelperResend = ItemTouchHelper(swipeHandlerResend)
        itemTouchHelperResend.attachToRecyclerView(crimeRecyclerView)

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.ip_configuration ->{
                    val alert = AlertDialog.Builder(requireContext())
                    val edittext = EditText(requireContext())
                    edittext.text = SpannableStringBuilder(getIpFromShared());
                    alert.setMessage(R.string.ip_сщташпгкфешщт)
                    alert.setTitle("Сервер")

                    alert.setView(edittext)

                    alert.setPositiveButton(
                        "Готово"
                    ) { dialog, whichButton -> //What ever you want to do with the value
                        val ip = edittext.text.toString()
                        val preferences: SharedPreferences =
                            PreferenceManager.getDefaultSharedPreferences(requireContext())
                        val editor = preferences.edit()
                        editor.putString("ip", ip)
                        editor.apply()
                        ApiClient.reBuildRetrofit(ip)
                    }

                    alert.setNegativeButton(
                        "Отмена"
                    ) { dialog, whichButton ->
                        // what ever you want to do with No option.
                    }

                    alert.show()


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


        drawerLayout.addDrawerListener(object : DrawerListener {
            /**
             * Called when a drawer's position changes.
             *
             * @param slideOffset The new offset of this drawer within its range, from 0-1
             * Example when you slide drawer from left to right, slideOffset will increase from 0 - 1 (0 when drawer closed and 1 when drawer display full)
             */
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
//                crimeRecyclerView.visibility = View.GONE
            }
            override fun onDrawerOpened(drawerView: View) {
                crimeRecyclerView.visibility = View.GONE
            }

            override fun onDrawerClosed(drawerView: View) {
                crimeRecyclerView.visibility = View.VISIBLE
            }

            /**
             * Called when the drawer motion state changes. The new state will
             * be one of [.STATE_IDLE], [.STATE_DRAGGING] or [.STATE_SETTLING].
             */
            override fun onDrawerStateChanged(newState: Int) {
                if (newState == 1){
                    crimeRecyclerView.visibility = View.VISIBLE
                }
                Log.i("OFFSET", newState.toString())
            }
        })

        getIpFromShared()?.let { ApiClient.reBuildRetrofit(it) }



        mGoogleApiClient = GoogleApiClient.Builder(requireContext())
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build()

        mLocationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        Log.d("gggg","uooo");
        checkLocation()

        return view
    }




    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        this.menu = menu
        inflater.inflate(R.menu.fragment_crime_list, menu)
        menu.findItem(R.id.zone_1).setChecked(true)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.i("MENU", "${item}")
        return when (item.itemId) {

            R.id.flash ->{
                if (cameraxHelper.cameraInfo?.torchState?.value == TorchState.ON) {
                    cameraxHelper.cameraControl?.enableTorch(false)
                    item.setIcon(R.drawable.ic_flash_off)
                } else {
                    cameraxHelper.cameraControl?.enableTorch(true)
                    item.setIcon(R.drawable.ic_flash_on)
                }
                true
            }
            R.id.zone_1 ->{
                zoneChange(1, menu, item)
                true
            }
            R.id.zone_2 ->{
                zoneChange(2, menu, item)
                true
            }
            R.id.zone_3 ->{
                zoneChange(3, menu, item)
                true
            }
            R.id.zone_4 ->{
                zoneChange(4, menu, item)
                true
            }
            R.id.zone_5 ->{
                zoneChange(5, menu, item)
                true
            }
            R.id.zone_6 ->{
                zoneChange(6, menu, item)
                true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }





    val selectImageFromGalleryResult = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            //todo image picker

            Log.i("Image_picker", uri.path.toString())
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
                ApiClient.POST_img64(img64,img_path =  mFile3.path, img_plate_path = "None", zone = zone, long = 0.0, lat = 0.0)
            } catch (e: IOException) {
                Log.e("APP_LOG", "Exception in photoCallback", e)
            }

        }
    }

    private fun pickPhoto() {
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
            notifyItemRemoved(position)
        }

        override fun getItemCount() = crimes.size
    }

    companion object{

        fun newInstance(): CrimeListFragment {
            return CrimeListFragment()
        }
    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val latitude = java.lang.Double.valueOf(intent.getStringExtra("latutide"))
            val longitude = java.lang.Double.valueOf(intent.getStringExtra("longitude"))

            try {
                latLng =LatLng(latitude,longitude)
                cameraxHelper.latLng = latLng
            } catch (e1: NullPointerException) {
                e1.printStackTrace()
            }

        }
    }


    override fun onDestroy() {
        super.onDestroy()
        context?.unregisterReceiver(broadcastReceiver)

    }


    override fun onResume(){
        super.onResume()
        context?.registerReceiver(broadcastReceiver, IntentFilter(LocationService.str_receiver));
        if (androidx.core.app.ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && androidx.core.app.ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
    }
    override fun onPause() {
        super.onPause()

    }

    override fun onStop() {
        super.onStop()
        if (mGoogleApiClient!!.isConnected()) {
            mGoogleApiClient!!.disconnect()
        }
    }

    override fun onStart() {
        super.onStart()
        if (mGoogleApiClient != null) {
            mGoogleApiClient!!.connect()
        }

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
        if(File(crime.img_path).exists() && crime.img_path != "") {
            val bOut2 = ByteArrayOutputStream()
            val bm = BitmapFactory.decodeFile(crime.img_path)
            bm.compress(Bitmap.CompressFormat.JPEG, 100, bOut2)
            img64_full = Base64.encodeToString(bOut2.toByteArray(), Base64.DEFAULT)
            crime.date = Calendar.getInstance().time
            ApiClient.POST_img64(img64_full.toString(), crime)
        }
        else{
            Log.e("RESEND CRIME", "not deleted")
        }


    }



    private fun getIpFromShared(): String? {
        val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val url = preferences.getString("ip", "")
        if (!url.equals("", ignoreCase = true)) {
            return url
        }
        else{
            return "http://95.182.74.37:1234/"
        }
    }

    fun zoneChange(zon: Int, menu: Menu, item: MenuItem){
        cameraxHelper.zone = zon
        zone = zon
        item.isChecked = !item.isChecked
        when(zon){
            1 ->{
                menu.findItem(R.id.zone_show).setIcon(R.drawable.ic_first_zone)
                menu.findItem(R.id.zone_1).setChecked(true)
                menu.findItem(R.id.zone_2).setChecked(false)
                menu.findItem(R.id.zone_3).setChecked(false)
                menu.findItem(R.id.zone_4).setChecked(false)
                menu.findItem(R.id.zone_5).setChecked(false)
                menu.findItem(R.id.zone_6).setChecked(false)
            }
            2 ->{
                menu.findItem(R.id.zone_show).setIcon(R.drawable.ic_second_zone)
                menu.findItem(R.id.zone_1).setChecked(false)
                menu.findItem(R.id.zone_2).setChecked(true)
                menu.findItem(R.id.zone_3).setChecked(false)
                menu.findItem(R.id.zone_4).setChecked(false)
                menu.findItem(R.id.zone_5).setChecked(false)
                menu.findItem(R.id.zone_6).setChecked(false)
            }
            3 ->{
                menu.findItem(R.id.zone_show).setIcon(R.drawable.ic_third_zone)
                menu.findItem(R.id.zone_1).setChecked(false)
                menu.findItem(R.id.zone_2).setChecked(false)
                menu.findItem(R.id.zone_3).setChecked(true)
                menu.findItem(R.id.zone_4).setChecked(false)
                menu.findItem(R.id.zone_5).setChecked(false)
                menu.findItem(R.id.zone_6).setChecked(false)
            }
            4 ->{
                menu.findItem(R.id.zone_show).setIcon(R.drawable.ic_firth_zone)
                menu.findItem(R.id.zone_1).setChecked(false)
                menu.findItem(R.id.zone_2).setChecked(false)
                menu.findItem(R.id.zone_3).setChecked(false)
                menu.findItem(R.id.zone_4).setChecked(true)
                menu.findItem(R.id.zone_5).setChecked(false)
                menu.findItem(R.id.zone_6).setChecked(false)
            }
            5 ->{
                menu.findItem(R.id.zone_show).setIcon(R.drawable.ic_fifth_zone)
                menu.findItem(R.id.zone_1).setChecked(false)
                menu.findItem(R.id.zone_2).setChecked(false)
                menu.findItem(R.id.zone_3).setChecked(false)
                menu.findItem(R.id.zone_4).setChecked(false)
                menu.findItem(R.id.zone_5).setChecked(true)
                menu.findItem(R.id.zone_6).setChecked(false)
            }
            6 ->{
                menu.findItem(R.id.zone_show).setIcon(R.drawable.ic_sixth_zone)
                menu.findItem(R.id.zone_1).setChecked(false)
                menu.findItem(R.id.zone_2).setChecked(false)
                menu.findItem(R.id.zone_3).setChecked(false)
                menu.findItem(R.id.zone_4).setChecked(false)
                menu.findItem(R.id.zone_5).setChecked(false)
                menu.findItem(R.id.zone_6).setChecked(true)
            }
        }

}

    override fun onConnected(p0: Bundle?) {



        if (androidx.core.app.ActivityCompat.checkSelfPermission(
                requireContext(),
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && androidx.core.app.ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
        startLocationUpdates()
        if (mLocation == null) {
            startLocationUpdates()
        }
        if (mLocation != null) {
            Log.w("GPS", "lat-${mLocation!!.latitude}, long-${mLocation!!.longitude}")
            // mLatitudeTextView.setText(String.valueOf(mLocation.getLatitude()));
            //mLongitudeTextView.setText(String.valueOf(mLocation.getLongitude()));
        } else {
            Toast.makeText(context, "Не могу найти местоположение", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnectionSuspended(p0: Int) {
        mGoogleApiClient!!.connect()
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.i("GPS", "Ошибка, не могу найти местополежение " + p0.getErrorCode())
    }

    override fun onLocationChanged(p0: Location?) {

        val msg = "Обновляю местоположение: " +
                p0?.let { java.lang.Double.toString(it.latitude) } + "," +
                p0?.let { java.lang.Double.toString(it.longitude) }
        Log.i("GPS", msg)
//        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        // You can now create a LatLng Object for use with maps
//        latLng = p0?.let { LatLng(it.latitude, p0.longitude) }!!
//        cameraxHelper.latLng = latLng
    }

    protected fun startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(UPDATE_INTERVAL)
            .setFastestInterval(FASTEST_INTERVAL)
        // Request location updates

        if (androidx.core.app.ActivityCompat.checkSelfPermission(
                requireContext(),
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && androidx.core.app.ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "Ищу местоположение",Toast.LENGTH_SHORT).show()
            return
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
            mLocationRequest, this)
        Log.d("GPS", "Нашел местоположение")
    }


    private fun checkLocation(): Boolean {
        if (!isLocationEnabled)
            showAlert()
        return isLocationEnabled
    }

    private fun showAlert() {
        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle("Enable Location")
            .setMessage("Использование вашего местоположения выключено'.\nВключите его " + "чтобы пользоваться приложением")
            .setPositiveButton("Настройки") { paramDialogInterface, paramInt ->
                val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(myIntent)
            }
            .setNegativeButton("Выйти") { paramDialogInterface, paramInt -> }
        dialog.show()
    }



}



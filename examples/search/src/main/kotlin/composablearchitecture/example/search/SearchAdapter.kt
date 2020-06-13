package composablearchitecture.example.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import composablearchitecture.example.search.databinding.LocationItemBinding
import kotlin.math.roundToInt

class LocationViewHolder(
    private val binding: LocationItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(newLocation: Location, state: SearchState) {
        if (state.locationWeather != null && state.locationWeather.id == newLocation.id) {
            val weather = state.locationWeather.consolidatedWeather
                .joinToString("\n") {
                    "  ${it.applicableDate.dayOfWeek}, ${it.theTemp.roundToInt()}℃, ${it.weatherStateName}"
                }
            binding.weatherText = "${newLocation.title}\n${weather}"
        } else {
            binding.weatherText = newLocation.title
        }
        binding.showProgress = newLocation == state.locationWeatherRequestInFlight
        binding.location = newLocation
        binding.executePendingBindings()
    }
}

class SearchAdapter(
    val onLocationTap: (Location) -> Unit
) : RecyclerView.Adapter<LocationViewHolder>() {

    private lateinit var state: SearchState

    fun update(newState: SearchState) {
        state = newState
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LocationItemBinding.inflate(inflater, parent, false)
        val holder = LocationViewHolder(binding)
        binding.adapter = this
        return holder
    }

    override fun getItemCount(): Int = state.locations.size

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(state.locations[position], state)
    }
}

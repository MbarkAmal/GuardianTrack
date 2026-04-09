package com.example.guardiantrack.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.guardiantrack.databinding.FragmentHistoryBinding
import com.example.guardiantrack.data.model.Incident

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HistoryAdapter
    private val incidentList = mutableListOf<Incident>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //  Prepare fake data (for test)
        val now = System.currentTimeMillis()
        incidentList.add(Incident(timestamp = now - 3_600_000, type = "CHUTE_DETECTEE",     latitude = 0.0, longitude = 0.0, isSynced = true))
        incidentList.add(Incident(timestamp = now - 1_800_000, type = "RYTHME_CARDIAQUE",   latitude = 0.0, longitude = 0.0, isSynced = false))
        incidentList.add(Incident(timestamp = now,             type = "BATTERIE_CRITIQUE",  latitude = 0.0, longitude = 0.0, isSynced = false))

        //  Setup adapter
        adapter = HistoryAdapter(incidentList)

        //  Setup RecyclerView
        binding.recyclerHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHistory.adapter = adapter

        //  Swipe to delete
        val itemTouchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ) = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    adapter.removeItem(viewHolder.adapterPosition)
                }
            }
        )

        itemTouchHelper.attachToRecyclerView(binding.recyclerHistory)

        //  Export buttons (just test)
        binding.btnExportCsv.setOnClickListener {
            // TODO: export CSV
        }

        binding.btnExportTxt.setOnClickListener {
            // TODO: export TXT
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
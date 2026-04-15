package com.example.guardiantrack.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.guardiantrack.databinding.FragmentHistoryBinding
import com.example.guardiantrack.data.model.IncidentEntity
import com.example.guardiantrack.util.HistoryExporter
import com.example.guardiantrack.viewmodel.HistoryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: HistoryAdapter
    private val incidentList = mutableListOf<IncidentEntity>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup adapter with empty list initially
        adapter = HistoryAdapter(incidentList)

        // Setup RecyclerView
        binding.recyclerHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHistory.adapter = adapter

        // Observe ViewModel real data
        viewModel.incidents.observe(viewLifecycleOwner) { incidents ->
            if (incidents != null) {
                incidentList.clear()
                incidentList.addAll(incidents)
                adapter.notifyDataSetChanged()
            }
        }

        //  Swipe to delete
        val itemTouchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ) = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val incident = adapter.getItem(position)
                        
                        // Delete from database
                        viewModel.deleteIncident(incident)
                        
                        // Note: LiveData will handle the list update automatically
                    }
                }
            }
        )

        itemTouchHelper.attachToRecyclerView(binding.recyclerHistory)

        //  Export buttons
        binding.btnExportCsv.setOnClickListener {
            if (incidentList.isEmpty()) {
                Toast.makeText(requireContext(), "Aucun incident à exporter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val success = HistoryExporter.exportToCsv(requireContext(), incidentList)
            if (success) {
                Toast.makeText(requireContext(), "Export CSV réussi (Dossier Documents)", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Échec de l'export CSV", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnExportTxt.setOnClickListener {
            if (incidentList.isEmpty()) {
                Toast.makeText(requireContext(), "Aucun incident à exporter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val success = HistoryExporter.exportToTxt(requireContext(), incidentList)
            if (success) {
                Toast.makeText(requireContext(), "Export TXT réussi (Dossier Documents)", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Échec de l'export TXT", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
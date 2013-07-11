package br.ufmt.periscope.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.ChartSeries;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.report.MainIPCReport;
import br.ufmt.periscope.report.Pair;

@ManagedBean
@ViewScoped
public class MainIPCController {

	private @Inject
	@CurrentProject
	Project currentProject;
	private @Inject
	MainIPCReport report;
	private CartesianChartModel model;
	private List<Pair> pairs;

	private boolean klass;
	private boolean subKlass;
	private boolean group;
	private boolean subGroup;
	
	private int limit = 5;

	@PostConstruct
	public void init() {
		klass = false;
		subKlass = false;
		group = false;
		subGroup = false;
		mainIpcChart();
	}

	public void update() {
		if (!klass) {
			// classe nao esta selecionada
			// buscar secao
			subKlass = false;
			group = false;
			subGroup = false;
		} else if (!subKlass) {
			// classe selecionada e subclasse nao esta
			// buscar classe
			group = false;
			subGroup = false;
		} else if (!group) {
			// classe e subclasse selecionadas e grupo nao selecionado
			// buscar subclasse
			subGroup = false;
		} else if (!subGroup) {
			// classe, subclasse e grupo selecionado, subgrupo nao selecioando
			// buscar grupo
		} else {
			// tudo selecionado
			// buscar subgrupo
		}

		mainIpcChart();
	}

	public void mainIpcChart() {
		model = new CartesianChartModel();
		ChartSeries series = report.ipcCount(currentProject, klass, subKlass,
				group, subGroup, limit);

		model.addSeries(series);

		setPairs(new ArrayList<Pair>());

		for (Object key : series.getData().keySet()) {
			Number value = series.getData().get(key);
			getPairs().add(new Pair(key, value));
		}

		Collections.reverse(pairs);
	}

	public List<Pair> getPairs() {
		return pairs;
	}

	public void setPairs(List<Pair> pairs) {
		this.pairs = pairs;
	}

	public CartesianChartModel getModel() {
		return model;
	}

	public void setModel(CartesianChartModel model) {
		this.model = model;
	}

	public boolean isKlass() {
		return klass;
	}

	public void setKlass(boolean klass) {
		this.klass = klass;
	}

	public boolean isSubKlass() {
		return subKlass;
	}

	public void setSubKlass(boolean subKlass) {
		this.subKlass = subKlass;
	}

	public boolean isGroup() {
		return group;
	}

	public void setGroup(boolean group) {
		this.group = group;
	}

	public boolean isSubGroup() {
		return subGroup;
	}

	public void setSubGroup(boolean subGroup) {
		this.subGroup = subGroup;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

}

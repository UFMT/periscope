package br.ufmt.periscope.report;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.model.chart.ChartSeries;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.ClassificationRepository;
import br.ufmt.periscope.util.Filters;

@Named
public class MainIPCReport {

    private @Inject
    ClassificationRepository repo;

    public ChartSeries ipcCount(Project currentProject, boolean klass,
            boolean subKlass, boolean group, boolean subGroup, int limit, Filters filtro, int classification) {

        ChartSeries series = new ChartSeries("Contagem classificação");

        List<Pair> i = repo.getMainIPC(currentProject, klass, subKlass, group, subGroup, limit, filtro, classification);

        Collections.reverse(i);

        for (Pair pair : i) {
            String ipc = (String) pair.getKey();
            Integer count = (Integer) pair.getValue();
            series.set(ipc, count);
        }

        return series;
    }

}

package net.flex.dci.otc.controller.ne.manager.service;

import java.util.List;
import java.util.Set;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.controller.ne.manager.dto.OperationResult;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;

/**
 * @version 1.0
 * @date 2023/4/11 14:12
 */
public interface NeManager {

    /**
     * register ne
     *
     * @param input
     * @return
     */
    String registerNe(String input);

    /**
     * create ne
     *
     * @param input
     * @return
     */
    String createNe(String input);


    /**
     * re register the ne
     *
     * @param neId
     * @return
     */
    String reRegisterNe(String neId, String friendlyName);


    /**
     * unregister the ne
     *
     * @param input unregister ne input
     * @return
     */
    String unregisteredNe(String input);

    void unSuperviseNe(String neId);


    /**
     * manager ne rpc eml-manager:manage-ne
     *
     * @return
     */
    String manageNe();

    /**
     * config ne
     *
     * @param input
     * @return
     */
    String configNe(String input);


    ConfigNeOutput configNe(String neId, Physical physical, List<TerminationPoint> tps);

    /**
     * upload ne
     *
     * @param input
     * @return
     */
    String uploadNe(String input);


    OperationResult mergeData(String neId) throws CommonException;

    /**
     * get ne data
     *
     * @param input
     * @return
     */
    String getNeData(String input);


    /**
     * remove the ne resource
     *
     * @param input
     * @return
     */
    String removeResource(String input);

    /**
     * compare ne
     *
     * @param input
     * @return
     */
    String compareNe(String input);


    String report1524TelemetryData(String input);

    String neSoftwareOperate(String input);

    String neDatabaseOperate(String input);

    void assignTelemetry2NeByNeIds(Set<String> needConfigNeIds);

    void reRegisterNeWithTimeout(String neId, String friendlyName);

    String operationLink(String input);

    String switchCUActiveStandby(String input);

    String uploadHistoryPm(String input);

    String channelAseRestore(String input);

    String batchConfigNe(String input);

    void rebalanceAllTelemetryServers();
}

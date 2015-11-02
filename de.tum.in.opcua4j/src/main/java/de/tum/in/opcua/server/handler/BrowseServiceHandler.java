package de.tum.in.opcua.server.handler;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.core.AddNodesRequest;
import org.opcfoundation.ua.core.AddNodesResponse;
import org.opcfoundation.ua.core.AddReferencesRequest;
import org.opcfoundation.ua.core.AddReferencesResponse;
import org.opcfoundation.ua.core.BrowseDescription;
import org.opcfoundation.ua.core.BrowseNextRequest;
import org.opcfoundation.ua.core.BrowseNextResponse;
import org.opcfoundation.ua.core.BrowsePath;
import org.opcfoundation.ua.core.BrowseRequest;
import org.opcfoundation.ua.core.BrowseResponse;
import org.opcfoundation.ua.core.BrowseResult;
import org.opcfoundation.ua.core.BrowseResultMask;
import org.opcfoundation.ua.core.DeleteNodesRequest;
import org.opcfoundation.ua.core.DeleteNodesResponse;
import org.opcfoundation.ua.core.DeleteReferencesRequest;
import org.opcfoundation.ua.core.DeleteReferencesResponse;
import org.opcfoundation.ua.core.NodeManagementServiceSetHandler;
import org.opcfoundation.ua.core.QueryFirstRequest;
import org.opcfoundation.ua.core.QueryFirstResponse;
import org.opcfoundation.ua.core.QueryNextRequest;
import org.opcfoundation.ua.core.QueryNextResponse;
import org.opcfoundation.ua.core.ReferenceDescription;
import org.opcfoundation.ua.core.RegisterNodesRequest;
import org.opcfoundation.ua.core.RegisterNodesResponse;
import org.opcfoundation.ua.core.RelativePathElement;
import org.opcfoundation.ua.core.StatusCodes;
import org.opcfoundation.ua.core.TranslateBrowsePathsToNodeIdsRequest;
import org.opcfoundation.ua.core.TranslateBrowsePathsToNodeIdsResponse;
import org.opcfoundation.ua.core.UnregisterNodesRequest;
import org.opcfoundation.ua.core.UnregisterNodesResponse;
import org.opcfoundation.ua.transport.EndpointServiceRequest;

import de.tum.in.opcua.server.core.Session;
import de.tum.in.opcua.server.handler.referencefilter.IReferenceFilter;
import de.tum.in.opcua.server.handler.referencefilter.RefDirectionFilter;
import de.tum.in.opcua.server.handler.referencefilter.RefTypeFilter;
import de.tum.in.opcua.server.handler.referencefilter.TargetNodeTypeFilter;

public class BrowseServiceHandler extends ServiceHandlerBase implements
		NodeManagementServiceSetHandler {

	private static final Logger LOG = Logger
			.getLogger(BrowseServiceHandler.class);

	@Override
	public void onAddNodes(
			EndpointServiceRequest<AddNodesRequest, AddNodesResponse> serviceReq)
			throws ServiceFaultException {

		initRequestContext(serviceReq);
		final AddNodesRequest req = serviceReq.getRequest();
		final AddNodesResponse resp = new AddNodesResponse();

		resp.setResponseHeader(buildErrRespHeader(req,
				StatusCodes.Bad_ServiceUnsupported));
		sendResp(serviceReq, resp);
	}

	@Override
	public void onAddReferences(
			EndpointServiceRequest<AddReferencesRequest, AddReferencesResponse> serviceReq)
			throws ServiceFaultException {

		initRequestContext(serviceReq);
		final AddReferencesRequest req = serviceReq.getRequest();
		final AddReferencesResponse resp = new AddReferencesResponse();

		resp.setResponseHeader(buildErrRespHeader(req,
				StatusCodes.Bad_ServiceUnsupported));
		sendResp(serviceReq, resp);
	}

	@Override
	public void onDeleteNodes(
			EndpointServiceRequest<DeleteNodesRequest, DeleteNodesResponse> serviceReq)
			throws ServiceFaultException {

		initRequestContext(serviceReq);
		final DeleteNodesRequest req = serviceReq.getRequest();
		final DeleteNodesResponse resp = new DeleteNodesResponse();

		resp.setResponseHeader(buildErrRespHeader(req,
				StatusCodes.Bad_ServiceUnsupported));
		sendResp(serviceReq, resp);
	}

	@Override
	public void onDeleteReferences(
			EndpointServiceRequest<DeleteReferencesRequest, DeleteReferencesResponse> serviceReq)
			throws ServiceFaultException {

		initRequestContext(serviceReq);
		final DeleteReferencesRequest req = serviceReq.getRequest();
		final DeleteReferencesResponse resp = new DeleteReferencesResponse();

		resp.setResponseHeader(buildErrRespHeader(req,
				StatusCodes.Bad_ServiceUnsupported));
		sendResp(serviceReq, resp);
	}

	@Override
	public void onBrowse(
			EndpointServiceRequest<BrowseRequest, BrowseResponse> serviceReq)
			throws ServiceFaultException {
		initRequestContext(serviceReq);
		final BrowseRequest req = serviceReq.getRequest();
		final BrowseResponse resp = new BrowseResponse();

		resp.setResponseHeader(buildRespHeader(req));

		// LOG.info("clients sends browserequest for " +
		// req.getNodesToBrowse().length + " nodes");
		List<BrowseResult> browseResults = new ArrayList<BrowseResult>();
		for (final BrowseDescription browseDesc : req.getNodesToBrowse()) {
			LOG.info("got browse request for nodeID " + browseDesc.getNodeId());
			browseResults.add(browse(browseDesc));
		}

		final int maxReferences = req.getRequestedMaxReferencesPerNode()
				.intValue();

		/*
		 * create continuation point. maxReferences == 0 --> no limit
		 */
		if (maxReferences > 0 && browseResults.size() > maxReferences) {
			LOG.info(String.format(
					"got more browse results (%d) than client wants (%d) ",
					browseResults.size(), maxReferences));
			browseResults = browseResults.subList(0, maxReferences - 1);

			// TODO create continuation point and safe it in the session object
			@SuppressWarnings("unused")
			final Session session = getSession(req);
		}

		resp.setResults(browseResults.toArray(new BrowseResult[browseResults
				.size()]));
		sendResp(serviceReq, resp);
	}

	/**
	 * This method does browsing for one single node
	 * 
	 * @param browseDesc
	 * @return
	 */
	private BrowseResult browse(BrowseDescription browseDesc) {
		// fetch all references.
		final List<ReferenceDescription> allReferences = server.getAddrSpace()
				.browseNode(browseDesc.getNodeId());

		/*
		 * there are several filters which are be applied on the fetched
		 * references because the client may want to restrict them:
		 * 
		 * 1.) filter them by reference direction 2.) filter them by the targets
		 * Node's NodeClass 3.) filter them by reference type
		 * 
		 * we do the filtering here because its a central point and so not every
		 * nodemanager has to deal with the filtering
		 */
		final List<IReferenceFilter> filters = new ArrayList<IReferenceFilter>();
		filters.add(new RefDirectionFilter());
		filters.add(new TargetNodeTypeFilter());
		filters.add(new RefTypeFilter());

		// apply every filter
		List<ReferenceDescription> filteredReferences = allReferences;
		for (final IReferenceFilter filter : filters) {
			filteredReferences = filter.filter(filteredReferences, browseDesc);
		}

		// filter fields of the remaining referenceDescriptions to match the
		// clients request. he may want just a view fields set
		final EnumSet<BrowseResultMask> resMask = BrowseResultMask
				.getSet(browseDesc.getResultMask());
		final List<ReferenceDescription> resultingDescriptions = new ArrayList<ReferenceDescription>();
		for (final ReferenceDescription refDesc : filteredReferences) {
			resultingDescriptions.add(filterRefDescFields(refDesc, resMask));
		}

		// create the result
		final BrowseResult result = new BrowseResult();
		result.setStatusCode(StatusCode.GOOD);
		result.setReferences(filteredReferences
				.toArray(new ReferenceDescription[resultingDescriptions.size()]));
		return result;
	}

	/**
	 * the client can decide which fields he want to be set in the resulting
	 * {@link ReferenceDescription}. This is done by a {@link BrowseResultMask}.
	 * This method sets only those fields, the client requested with its sent
	 * {@link BrowseResultMask}.
	 * 
	 * @param refDescToFilter
	 * @return
	 */
	private ReferenceDescription filterRefDescFields(
			ReferenceDescription refDescToFilter, EnumSet<BrowseResultMask> mask) {
		ReferenceDescription result;

		if (mask.contains(BrowseResultMask.All)) {
			// return all fields
			result = refDescToFilter;
		} else {
			result = new ReferenceDescription();
			result.setNodeId(refDescToFilter.getNodeId());

			// only return requested fields
			if (mask.contains(BrowseResultMask.ReferenceTypeId)) {
				result.setReferenceTypeId(refDescToFilter.getReferenceTypeId());
			}
			if (mask.contains(BrowseResultMask.IsForward)) {
				result.setIsForward(refDescToFilter.getIsForward());
			}
			if (mask.contains(BrowseResultMask.NodeClass)) {
				result.setNodeClass(refDescToFilter.getNodeClass());
			}
			if (mask.contains(BrowseResultMask.BrowseName)) {
				result.setBrowseName(refDescToFilter.getBrowseName());
			}
			if (mask.contains(BrowseResultMask.DisplayName)) {
				result.setDisplayName(refDescToFilter.getDisplayName());
			}
			if (mask.contains(BrowseResultMask.TypeDefinition)) {
				result.setTypeDefinition(refDescToFilter.getTypeDefinition());
			}
		}

		return result;
	}

	@Override
	public void onBrowseNext(
			EndpointServiceRequest<BrowseNextRequest, BrowseNextResponse> serviceReq)
			throws ServiceFaultException {

		initRequestContext(serviceReq);
		final BrowseNextRequest req = serviceReq.getRequest();
		final BrowseNextResponse resp = new BrowseNextResponse();

		LOG.info("---------------  got onbrowsenext request: " + req);

		resp.setResponseHeader(buildErrRespHeader(req,
				StatusCodes.Bad_ServiceUnsupported));
		sendResp(serviceReq, resp);
	}

	@Override
	public void onTranslateBrowsePathsToNodeIds(

			EndpointServiceRequest<TranslateBrowsePathsToNodeIdsRequest, TranslateBrowsePathsToNodeIdsResponse> serviceReq)
			throws ServiceFaultException {
		initRequestContext(serviceReq);
		final TranslateBrowsePathsToNodeIdsRequest req = serviceReq
				.getRequest();
		final TranslateBrowsePathsToNodeIdsResponse resp = new TranslateBrowsePathsToNodeIdsResponse();

		LOG.debug("====>> translate browse paths request");
		for (final BrowsePath path : req.getBrowsePaths()) {
			for (final RelativePathElement pathElement : path.getRelativePath()
					.getElements()) {
				LOG.debug(pathElement.getIncludeSubtypes() + ", "
						+ pathElement.getReferenceTypeId() + ", "
						+ pathElement.getTargetName().getName());
			}
		}

		// TODO implement this service. this is only relevant for clients who
		// programm against type definitions.
		resp.setResponseHeader(buildErrRespHeader(req,
				StatusCodes.Bad_QueryTooComplex));
		sendResp(serviceReq, resp);
	}

	@Override
	public void onRegisterNodes(
			EndpointServiceRequest<RegisterNodesRequest, RegisterNodesResponse> serviceReq)
			throws ServiceFaultException {

		LOG.info("called onRegisterNodes");

		initRequestContext(serviceReq);
		final RegisterNodesRequest req = serviceReq.getRequest();
		final RegisterNodesResponse resp = new RegisterNodesResponse();

		LOG.info("request: " + req.toString());

		resp.setResponseHeader(buildErrRespHeader(req,
				StatusCodes.Bad_ServiceUnsupported));
		sendResp(serviceReq, resp);
	}

	@Override
	public void onUnregisterNodes(
			EndpointServiceRequest<UnregisterNodesRequest, UnregisterNodesResponse> serviceReq)
			throws ServiceFaultException {

		initRequestContext(serviceReq);
		final UnregisterNodesRequest req = serviceReq.getRequest();
		final UnregisterNodesResponse resp = new UnregisterNodesResponse();

		resp.setResponseHeader(buildErrRespHeader(req,
				StatusCodes.Bad_ServiceUnsupported));
		sendResp(serviceReq, resp);
	}

	@Override
	public void onQueryFirst(
			EndpointServiceRequest<QueryFirstRequest, QueryFirstResponse> serviceReq)
			throws ServiceFaultException {

		initRequestContext(serviceReq);
		final QueryFirstRequest req = serviceReq.getRequest();
		final QueryFirstResponse resp = new QueryFirstResponse();

		resp.setResponseHeader(buildErrRespHeader(req,
				StatusCodes.Bad_ServiceUnsupported));
		sendResp(serviceReq, resp);
	}

	@Override
	public void onQueryNext(
			EndpointServiceRequest<QueryNextRequest, QueryNextResponse> serviceReq)
			throws ServiceFaultException {

		initRequestContext(serviceReq);
		final QueryNextRequest req = serviceReq.getRequest();
		final QueryNextResponse resp = new QueryNextResponse();

		resp.setResponseHeader(buildErrRespHeader(req,
				StatusCodes.Bad_ServiceUnsupported));
		sendResp(serviceReq, resp);
	}

}

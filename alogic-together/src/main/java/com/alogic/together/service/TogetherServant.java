package com.alogic.together.service;

import org.apache.commons.lang3.StringUtils;

import com.alogic.xscript.LogicletContext;
import com.alogic.xscript.Script;
import com.anysoft.util.Properties;
import com.anysoft.util.PropertiesConstants;
import com.logicbus.backend.AbstractServant;
import com.logicbus.backend.Context;
import com.logicbus.backend.message.JsonMessage;
import com.logicbus.models.servant.ServiceDescription;

/**
 * TogetherServant
 * 
 * @author duanyy
 *
 */
public class TogetherServant extends AbstractServant {
	protected Script script = null;
	protected String service;
	
	@Override
	protected void onDestroy() {
		
	}

	@Override
	protected void onCreate(ServiceDescription sd) {
		Properties p = sd.getProperties();
		service = sd.getPath();
		String bootstrap = PropertiesConstants.getString(p,"bootstrap","",true);
		if (StringUtils.isEmpty(bootstrap)){
			String config = PropertiesConstants.getString(p,"script","");
			if (StringUtils.isNotEmpty(config)){
				script = Script.create(config, p);
			}
		}else{
			String config = PropertiesConstants.getString(p,"script","");
			if (StringUtils.isNotEmpty(config)){
				script = Script.create(bootstrap, config, p);
			}
		}
	}

	@Override
	protected int onJson(Context ctx) throws Exception {
		if (script != null){
			JsonMessage msg = (JsonMessage) ctx.asMessage(JsonMessage.class);
			
			LogicletContext logicletContext = new SevantLogicletContext(ctx);
			logicletContext.setObject("$context", ctx);
			logicletContext.SetValue("$service", service);
			try {
				script.execute(msg.getRoot(),msg.getRoot(), logicletContext, null);
			}finally{
				logicletContext.removeObject("$context");
			}
		}else{
			ctx.asMessage(JsonMessage.class);
		}
		return 0;
	}

}

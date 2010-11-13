package org.mhoffman

import java.util.Properties
import net.lag.configgy.Configgy
import org.springframework.core.io.Resource
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer

/**
 * There is no reason for this class to be in this source tree, except I pulled it off of the Akka mailing list (from the
 * link below) and didn't want to forget about it; I think it could be handy later.
 * 
 * From http://groups.google.com/group/akka-user/browse_thread/thread/3082d936e249f5a7
 */
class ConfiggyPropertyPlaceholderConfigurator extends PropertyPlaceholderConfigurer {
  var confResource: Resource = null

  def setConfig(conf: Resource) = confResource = conf

  private def loadConf(props: Properties) {
    if (confResource == null) throw new IllegalArgumentException("Property 'conf' must be set")
    Configgy.configure(confResource.getFile.getPath)
    val config = Configgy.config
    config.asMap.foreach {case (k, v) => props.put(k, v)}
  }

  override protected def loadProperties(props: Properties) {
    loadConf(props)
    super.loadProperties(props)
  }

}
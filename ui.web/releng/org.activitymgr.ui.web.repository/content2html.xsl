<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="html" omit-xml-declaration="yes" indent="yes"/>
  <xsl:strip-space elements="*"/>
  <xsl:param name="docs_list" />
  <xsl:param name="help_ref" />

  <xsl:template match="/">
    <html xmlns="http://www.w3.org/1999/xhtml">
      <xsl:apply-templates select="repository"/>
    </html>
  </xsl:template>

  <xsl:template match="repository">
    <head>
      <title><xsl:value-of select="@name"/></title>
    </head>
    <body>
      <h1><xsl:value-of select="@name"/></h1>
      
      <p>This URL is an Eclipse <b>software repository</b>; 
      you must use it in Eclipse (<a 
      href="https://help.eclipse.org/topic/org.eclipse.platform.doc.user/tasks/tasks-127.htm"
      >see how</a>).
      </p>
      
      
      <table border="0">
      
      <tr>
          <td colspan="2">
      
      <xsl:if test="string-length($docs_list) > 0 or $help_ref">
        <h2>Documentation</h2>
        <xsl:if test="$help_ref" >
        <p>
        You can view the <a href="help/index.html">online User Guide</a>.
        </p>
        </xsl:if>
        <xsl:if test="string-length($docs_list) > 0">
        <p>Following PDF files contain more information on repository content.    
          <ul><!-- tokenize is not supported in xslt 1 -->
            <xsl:call-template name="list_docs">
              <xsl:with-param name="values" select="$docs_list"/>
            </xsl:call-template>
          </ul>
        </p>
        </xsl:if>
      </xsl:if>

          
      <h2>Dependencies</h2>
      <p>
        Following Target Platform descriptions lists used elements to build provided components:
        <ul>
          <li><a href="tpd/runtime.tpd">Runtime</a>: Dependencies for executing components.</li>
          <li><a href="tpd/build.tpd">Build</a>: Dependencies for building and testing components.</li>
          <li><a href="tpd/sdk.tpd">SDK</a>: Dependencies to debug components.</li>
        </ul> 
      </p>
          </td>
        </tr>

        <tr>
          <td colspan="2">        
            <h2>Features</h2>
          </td>
        </tr>
        <xsl:apply-templates select="//provided[@namespace='org.eclipse.update.feature']">
          <xsl:with-param name="path" select=" 'features' " />
          <xsl:sort select="@name"/>
        </xsl:apply-templates>

        <tr>
          <td colspan="2">
            <p>&#160;</p>
          </td>
        </tr>
        <tr>
          <td colspan="2">
            <h2>Plugins</h2>
          </td>
        </tr>
        <xsl:apply-templates select="//provided[@namespace='osgi.bundle']">
          <xsl:with-param name="path" select=" 'plugins' " />
          <xsl:sort select="@name"/>
        </xsl:apply-templates>
      </table>
    </body>
  </xsl:template>

  <xsl:template match="provided">
    <xsl:param name = "path" />
    <tr>
      <td><a href="{$path}/{@name}_{@version}.jar" ><xsl:value-of select="@name"/></a></td>
      <td><xsl:value-of select="@version"/></td>
    </tr>
  </xsl:template>

  <xsl:template name="list_docs">
    <xsl:param name="values" />
    
    <xsl:if test="string-length($values)">
    <xsl:variable name="item" select="substring-before(concat($values,';'),';')"/>
      <li><a href="docs/{$item}"><xsl:value-of select="substring-before($item, '.pdf')" /></a></li>
      <xsl:call-template name="list_docs">
        <xsl:with-param name="values" select="substring-after($values, ';')"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>

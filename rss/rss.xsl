<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:template match="/">
<html>
<body>
Channels<br></br>
<xsl:apply-templates/>
</body>
</html>
</xsl:template>
<xsl:template match="documentcollection/document/rss">
<p>Link: <br/>
  <xsl:element name="a">
    <xsl:attribute name="href">
        <xsl:value-of select="../@location"/>
    </xsl:attribute>
    <xsl:value-of select="../@location"/>
</xsl:element>
<xsl:apply-templates select="channel"/>
</p>
</xsl:template>
<xsl:template match="channel">
<xsl:for-each select="item">
<xsl:choose>
<xsl:when test="title/text() and (title[contains(text(),'war')] or title[contains(text(),'peace')])">
<xsl:if test="title and title/text()">
<p>Title: <xsl:value-of select="title"/></p>
</xsl:if>
<xsl:if test="link and link/text()">
<p>Link:<xsl:value-of select="link"/></p>
</xsl:if>
<xsl:if test="description/text()">
<p>Description:<xsl:value-of select="description"/></p>
</xsl:if>
</xsl:when>
<xsl:otherwise>
<xsl:if test="description/text() and (description[contains(text(),'war')] or description[contains(text(),'peace')])">
<xsl:if test="title and title/text()">
<p>Title: <xsl:value-of select="title"/></p>
</xsl:if>
<xsl:if test="link and link/text()">
<p>Link:<xsl:value-of select="link"/></p>
</xsl:if>
<xsl:if test="description/text()">
<p>Description:<xsl:value-of select="description"/></p>
</xsl:if>
</xsl:if>
</xsl:otherwise>
</xsl:choose>
</xsl:for-each>
</xsl:template>
</xsl:stylesheet>

<?xml version="1.0" encoding="utf-8" ?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">


<xsl:template match="/">
    <html>
        <head>
            <title>nginx-rtmp statistics</title>
        </head>
        <body>
            <xsl:apply-templates select="rtmp"/>
        </body>
    </html>
</xsl:template>

<xsl:template match="rtmp">
    <table cellspacing="1" cellpadding="5" text-align="center" font-size="10pt" width="100%">
        <tr bgcolor="#d2eaf1">
            <th>rtmp connection</th>
            <th>client</th>
            <th>bw in</th>
            <th>bw out</th>
            <th>rate in</th>
            <th>rate out</th>
            <th>size</th>
            <th>frame/s</th>
            <th>video</th>
            <th>audio</th>
            <th>state</th>
            <th>time</th>
        </tr>
        <tr>
            <td colspan="2">accepted: <xsl:value-of select="naccepted"/></td>
            <td>
                <xsl:call-template name="showsize">
                    <xsl:with-param name="size" select="in"/>
                </xsl:call-template>
            </td>
            <td>
               <xsl:call-template name="showsize">
                   <xsl:with-param name="size" select="out"/>
               </xsl:call-template>
           </td>
            <td>
               <xsl:call-template name="showsize">
                   <xsl:with-param name="size" select="bwin"/>
                   <xsl:with-param name="bits" select="1"/>
                   <xsl:with-param name="persec" select="1"/>
               </xsl:call-template>
           </td>
           <td>
               <xsl:call-template name="showsize">
                   <xsl:with-param name="size" select="bwout"/>
                   <xsl:with-param name="bits" select="1"/>
                   <xsl:with-param name="persec" select="1"/>
               </xsl:call-template>
           </td>
           <td colspan="5"/>
           <td>
            <xsl:call-template name="showtime">
                <xsl:with-param name="time" select="/rtmp/uptime * 1000"/>
            </xsl:call-template>
        </td>
    </tr>
    <xsl:apply-templates select="server"/>
</table>
</xsl:template>

<xsl:template match="server">
    <xsl:apply-templates select="application"/>
</xsl:template>

<xsl:template match="application">
    <tr bgcolor="#d2eaf1">
        <td>
            <b><xsl:value-of select="name"/></b>
        </td>
    </tr>
    <xsl:apply-templates select="live"/>
    <xsl:apply-templates select="play"/>
</xsl:template>

<xsl:template match="live">
    <tr bgcolor="#d9f4ff">
        <td>
            <i>live streams</i>
        </td>
        <td align="middle">
            <xsl:value-of select="nclients"/>
        </td>
    </tr>
    <xsl:apply-templates select="stream"/>
</xsl:template>

<xsl:template match="play">
    <tr bgcolor="#d9f4ff">
        <td>
            <i>vod streams</i>
        </td>
        <td align="middle">
            <xsl:value-of select="nclients"/>
        </td>
    </tr>
    <xsl:apply-templates select="stream"/>
</xsl:template>

<xsl:template match="stream">
    <tr valign="top">
        <xsl:attribute name="bgcolor">
            <xsl:choose>
                <xsl:when test="active">#bef2fb</xsl:when>
                <xsl:otherwise>#e6f9fc</xsl:otherwise>
            </xsl:choose>
        </xsl:attribute>
        <td>
            <a href="">
                <xsl:attribute name="onclick">
                    var d=document.getElementById('<xsl:value-of select="../../name"/>-<xsl:value-of select="name"/>');
                    d.style.display=d.style.display=='none'?'':'none';
                    return false
                </xsl:attribute>
                <xsl:value-of select="name"/>
                <xsl:if test="string-length(name) = 0">
                    [EMPTY]
                </xsl:if>
            </a>
        </td>
        <td align="middle"> <xsl:value-of select="nclients"/> </td>
        <td>
            <xsl:call-template name="showsize">
               <xsl:with-param name="size" select="in"/>
           </xsl:call-template>
        </td>
        <td>
            <xsl:call-template name="showsize">
                <xsl:with-param name="size" select="out"/>
            </xsl:call-template>
        </td>
        <td>
            <xsl:call-template name="showsize">
                <xsl:with-param name="size" select="bwin"/>
                <xsl:with-param name="bits" select="1"/>
                <xsl:with-param name="persec" select="1"/>
            </xsl:call-template>
        </td>
        <td>
            <xsl:call-template name="showsize">
                <xsl:with-param name="size" select="bwout"/>
                <xsl:with-param name="bits" select="1"/>
                <xsl:with-param name="persec" select="1"/>
            </xsl:call-template>
        </td>
        <td>
            <xsl:if test="meta/width &gt; 0">
                <xsl:value-of select="meta/width"/>x<xsl:value-of select="meta/height"/>
            </xsl:if>
        </td>
        <td align="middle"><xsl:value-of select="meta/framerate"/></td>
        <td>
            <xsl:value-of select="meta/video"/>
            <xsl:apply-templates select="meta/profile"/>
            <xsl:apply-templates select="meta/level"/>
        </td>
        <td><xsl:value-of select="meta/audio"/></td>
        <td><xsl:call-template name="streamstate"/></td>
        <td>
            <xsl:call-template name="showtime">
               <xsl:with-param name="time" select="time"/>
            </xsl:call-template>
        </td>
    </tr>
    <tr style="display:none">
        <xsl:attribute name="id">
            <xsl:value-of select="../../name"/>-<xsl:value-of select="name"/>
        </xsl:attribute>
        <td colspan="12" ngcolor="#e2f4fd">
            <table cellspacing="1" cellpadding="5" width="100%">
                <tr>
                    <th>id</th>
                    <th>state</th>
                    <th>address</th>
                    <th>flash version</th>
                    <th>page url</th>
                    <th>swf url</th>
                    <th>dropped</th>
                    <th>A-V</th>
                    <th>time</th>
                </tr>
                <xsl:apply-templates select="client"/>
            </table>
        </td>
    </tr>
</xsl:template>

<xsl:template name="showtime">
    <xsl:param name="time"/>

    <xsl:if test="$time &gt; 0">
        <xsl:variable name="sec">
            <xsl:value-of select="floor($time div 1000)"/>
        </xsl:variable>

        <xsl:if test="$sec &gt;= 86400">
            <xsl:value-of select="floor($sec div 86400)"/>d
        </xsl:if>

        <xsl:if test="$sec &gt;= 3600">
            <xsl:value-of select="(floor($sec div 3600)) mod 24"/>h
        </xsl:if>

        <xsl:if test="$sec &gt;= 60">
            <xsl:value-of select="(floor($sec div 60)) mod 60"/>m
        </xsl:if>

        <xsl:value-of select="$sec mod 60"/>s
    </xsl:if>
</xsl:template>

<xsl:template name="showsize">
    <xsl:param name="size"/>
    <xsl:param name="bits" select="0" />
    <xsl:param name="persec" select="0" />
    <xsl:variable name="sizen">
        <xsl:value-of select="floor($size div 1024)"/>
    </xsl:variable>
    <xsl:choose>
        <xsl:when test="$sizen &gt;= 1073741824">
            <xsl:value-of select="format-number($sizen div 1073741824,'#.###')"/> T</xsl:when>

        <xsl:when test="$sizen &gt;= 1048576">
            <xsl:value-of select="format-number($sizen div 1048576,'#.###')"/> G</xsl:when>

        <xsl:when test="$sizen &gt;= 1024">
            <xsl:value-of select="format-number($sizen div 1024,'#.##')"/> M</xsl:when>
        <xsl:when test="$sizen &gt;= 0">
            <xsl:value-of select="$sizen"/> K</xsl:when>
    </xsl:choose>
    <xsl:if test="string-length($size) &gt; 0">
        <xsl:choose>
            <xsl:when test="$bits = 1">b</xsl:when>
            <xsl:otherwise>B</xsl:otherwise>
        </xsl:choose>
        <xsl:if test="$persec = 1">/s</xsl:if>
    </xsl:if>
</xsl:template>

<xsl:template name="streamstate">
    <xsl:choose>
        <xsl:when test="active">active</xsl:when>
        <xsl:otherwise>idle</xsl:otherwise>
    </xsl:choose>
</xsl:template>


<xsl:template name="clientstate">
    <xsl:choose>
        <xsl:when test="publishing">publishing</xsl:when>
        <xsl:otherwise>playing</xsl:otherwise>
    </xsl:choose>
</xsl:template>


<xsl:template match="client">
    <tr>
        <xsl:attribute name="bgcolor">
            <xsl:choose>
                <xsl:when test="publishing">#e2f4fd</xsl:when>
                <xsl:otherwise>#e2f4fd</xsl:otherwise>
            </xsl:choose>
        </xsl:attribute>
        <td><xsl:value-of select="id"/></td>
        <td><xsl:call-template name="clientstate"/></td>
        <td><xsl:value-of select="address"/></td>
        <td><xsl:value-of select="flashver"/></td>
        <td>
            <a target="_blank">
                <xsl:attribute name="href">
                    <xsl:value-of select="pageurl"/>
                </xsl:attribute>
                <xsl:value-of select="pageurl"/>
            </a>
        </td>
        <td><xsl:value-of select="swfurl"/></td>
        <td><xsl:value-of select="dropped"/></td>
        <td><xsl:value-of select="avsync"/></td>
        <td>
            <xsl:call-template name="showtime">
               <xsl:with-param name="time" select="time"/>
            </xsl:call-template>
        </td>
    </tr>
</xsl:template>

<xsl:template match="publishing">
    publishing
</xsl:template>

<xsl:template match="active">
    active
</xsl:template>

<xsl:template match="profile">
    / <xsl:value-of select="."/>
</xsl:template>

<xsl:template match="level">
    / <xsl:value-of select="."/>
</xsl:template>

</xsl:stylesheet>

/**
 * This file Copyright (c) 2005-2010 Aptana, Inc. This program is
 * dual-licensed under both the Aptana Public License and the GNU General
 * Public license. You may elect to use one or the other of these licenses.
 * 
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT. Redistribution, except as permitted by whichever of
 * the GPL or APL you select, is prohibited.
 *
 * 1. For the GPL license (GPL), you can redistribute and/or modify this
 * program under the terms of the GNU General Public License,
 * Version 3, as published by the Free Software Foundation.  You should
 * have received a copy of the GNU General Public License, Version 3 along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Aptana provides a special exception to allow redistribution of this file
 * with certain other free and open source software ("FOSS") code and certain additional terms
 * pursuant to Section 7 of the GPL. You may view the exception and these
 * terms on the web at http://www.aptana.com/legal/gpl/.
 * 
 * 2. For the Aptana Public License (APL), this program and the
 * accompanying materials are made available under the terms of the APL
 * v1.0 which accompanies this distribution, and is available at
 * http://www.aptana.com/legal/apl/.
 * 
 * You may view the GPL, Aptana's exception and additional terms, and the
 * APL in the file titled license.html at the root of the corresponding
 * plugin containing this source file.
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package org.python.pydev.django_templates.xml;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.source.ISourceViewer;

import com.aptana.editor.common.AbstractThemeableEditor;
import com.aptana.editor.common.CommonEditorPlugin;
import com.aptana.editor.common.CompositeSourceViewerConfiguration;
import com.aptana.editor.common.IPartitionerSwitchStrategy;
import com.aptana.editor.common.scripting.IContentTypeTranslator;
import com.aptana.editor.common.scripting.QualifiedContentType;
import com.aptana.editor.common.text.rules.CompositePartitionScanner;
import org.python.pydev.django_templates.DjPartitionerSwitchStrategy;
import org.python.pydev.django_templates.IDjConstants;
import com.aptana.editor.ruby.RubySourceConfiguration;
import com.aptana.editor.ruby.core.RubyDoubleClickStrategy;
import com.aptana.editor.xml.XMLSourceConfiguration;

/**
 * @author Max Stepanov
 */
public class DjXMLSourceViewerConfiguration extends CompositeSourceViewerConfiguration
{

	static
	{
		IContentTypeTranslator c = CommonEditorPlugin.getDefault().getContentTypeTranslator();
		c.addTranslation(new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_XML), new QualifiedContentType(
		        IDjConstants.TOPLEVEL_DJANGO_TEMPLATES_XML_SCOPE)); 
		
		c.addTranslation(new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_XML,
				CompositePartitionScanner.START_SWITCH_TAG), new QualifiedContentType(
		        IDjConstants.TOPLEVEL_DJANGO_TEMPLATES_XML_SCOPE, IDjConstants.EMBEDDED_DJANGO_TEMPLATES_TAG_SCOPE_XML)); 
		
		c.addTranslation(new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_XML,
				CompositePartitionScanner.END_SWITCH_TAG), new QualifiedContentType(
		        IDjConstants.TOPLEVEL_DJANGO_TEMPLATES_XML_SCOPE, IDjConstants.EMBEDDED_DJANGO_TEMPLATES_TAG_SCOPE_XML));
	}

	private RubyDoubleClickStrategy fDoubleClickStrategy;

	public DjXMLSourceViewerConfiguration(IPreferenceStore preferences, AbstractThemeableEditor editor)
	{
		super(XMLSourceConfiguration.getDefault(), RubySourceConfiguration.getDefault(), preferences, editor);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.common.CompositeSourceViewerConfiguration#getTopContentType()
	 */
	@Override
	protected String getTopContentType()
	{
		return IDjConstants.CONTENT_TYPE_DJANGO_XML;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.common.CompositeSourceViewerConfiguration#getPartitionerSwitchStrategy()
	 */
	@Override
	protected IPartitionerSwitchStrategy getPartitionerSwitchStrategy()
	{
		return DjPartitionerSwitchStrategy.getDefault();
	}

	protected String getStartEndTokenType()
	{
		return "punctuation.section.embedded.django_templates"; //$NON-NLS-1$
	}
	
	@Override
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType)
	{
		if (fDoubleClickStrategy == null)
		{
			fDoubleClickStrategy = new RubyDoubleClickStrategy();
		}
		return fDoubleClickStrategy;
	}

}
